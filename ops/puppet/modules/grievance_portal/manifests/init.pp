# @summary Provisions Tomcat 10 on Ubuntu 24.04 and deploys a numbered release of the Policyholder Grievance Portal.
#
# The specification this class implements is docs/07-server-specification.md; releases, the health
# check and rollback are described in docs/08-provisioning-and-rollback.md.
#
# @param release         Name of the release to run, for example 1.0.0. A release is a stored WAR and never changes.
# @param war_source      WAR to store as that release. Optional when the node already holds the release (a rollback).
# @param http_port       Port Tomcat listens on.
# @param env_name        Label shown in the page footer and in /health (GP_ENV_NAME).
# @param java_package    Package that provides the Java runtime.
# @param java_home       JAVA_HOME that belongs to java_package.
# @param data_dir        Folder for the H2 database files (GP_DB_PATH is set to a file inside it).
# @param heap_min        JVM -Xms.
# @param heap_max        JVM -Xmx.
# @param health_attempts How many times the health check asks /health, three seconds apart, before it gives up.
class grievance_portal (
  String              $release         = '1.0.0',
  Optional[String]    $war_source      = undef,
  Integer[1024,65535] $http_port       = 8083,
  String              $env_name        = 'puppet-node',
  String              $java_package    = 'openjdk-17-jre-headless',
  String              $java_home       = '/usr/lib/jvm/java-17-openjdk-amd64',
  String              $data_dir        = '/var/lib/grievance-portal',
  String              $heap_min        = '128m',
  String              $heap_max        = '512m',
  Integer[1,200]      $health_attempts = 20,
) {
  $catalina_base = '/var/lib/tomcat10'
  $dropin_dir    = '/etc/systemd/system/tomcat10.service.d'
  $war_target    = "${catalina_base}/webapps/grievance-portal.war"
  $app_root      = '/opt/grievance-portal'
  $releases_dir  = "${app_root}/releases"
  $release_dir   = "${releases_dir}/${release}"
  $release_war   = "${release_dir}/grievance-portal.war"
  $stable_file   = "${app_root}/stable"
  $healthcheck   = '/usr/local/bin/gp-healthcheck'

  # --- Packages --------------------------------------------------------------------------
  # A fresh image has no package index, so refresh it, but only when the index is empty.
  # That keeps a second run free of changes.
  exec { 'apt-update-if-empty':
    command  => 'apt-get update',
    unless   => 'ls /var/lib/apt/lists/*Packages* > /dev/null 2>&1',
    path     => ['/usr/bin', '/bin'],
    provider => shell,
  }

  # Java comes first, so that the tomcat10 package finds the runtime it needs and does not pull in another.
  package { [$java_package, 'curl']:
    ensure  => installed,
    require => Exec['apt-update-if-empty'],
  }

  package { 'tomcat10':
    ensure  => installed,
    require => Package[$java_package],
  }

  # --- User and folders ------------------------------------------------------------------
  # The package creates the tomcat account; this asserts it exists and makes it own the data.
  user { 'tomcat':
    ensure  => present,
    require => Package['tomcat10'],
  }

  file { $data_dir:
    ensure  => directory,
    owner   => 'tomcat',
    group   => 'tomcat',
    mode    => '0750',
    require => User['tomcat'],
  }

  # --- Configuration files ---------------------------------------------------------------
  file { '/etc/default/tomcat10':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => epp('grievance_portal/tomcat10-default.epp', {
      java_home => $java_home,
      heap_min  => $heap_min,
      heap_max  => $heap_max,
      data_dir  => $data_dir,
      env_name  => $env_name,
    }),
    require => Package['tomcat10'],
    notify  => Service['tomcat10'],
  }

  file { '/etc/tomcat10/server.xml':
    ensure  => file,
    owner   => 'root',
    group   => 'tomcat',
    mode    => '0640',
    content => epp('grievance_portal/server.xml.epp', { http_port => $http_port }),
    require => Package['tomcat10'],
    notify  => Service['tomcat10'],
  }

  # The packaged unit makes most of the file system read-only; the database folder must stay writable.
  file { $dropin_dir:
    ensure  => directory,
    owner   => 'root',
    group   => 'root',
    mode    => '0755',
    require => Package['tomcat10'],
  }

  file { "${dropin_dir}/grievance-portal.conf":
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => epp('grievance_portal/systemd-dropin.epp', { data_dir => $data_dir }),
    notify  => Exec['systemd-daemon-reload'],
  }

  exec { 'systemd-daemon-reload':
    command     => 'systemctl daemon-reload',
    path        => ['/usr/bin', '/bin'],
    refreshonly => true,
    notify      => Service['tomcat10'],
  }

  # --- Releases --------------------------------------------------------------------------
  # Every release lives in its own folder and is stored once. replace => false makes a stored release
  # immutable: a rollback can rely on it being exactly what was deployed and checked.
  file { [$app_root, $releases_dir, $release_dir]:
    ensure => directory,
    owner  => 'root',
    group  => 'root',
    mode   => '0755',
  }

  if $war_source != undef {
    file { $release_war:
      ensure  => file,
      owner   => 'root',
      group   => 'root',
      mode    => '0644',
      source  => $war_source,
      replace => false,
      require => File[$release_dir],
    }
    $war_requires = [File[$release_war]]
  } else {
    $war_requires = [File[$release_dir]]
  }

  # --- The application -------------------------------------------------------------------
  # The deployed WAR is a copy of the chosen release. A changed WAR restarts Tomcat (auto-deploy is off in
  # server.xml), so that the health check below always judges the new release and never the one that was
  # running a moment before.
  file { $war_target:
    ensure  => file,
    owner   => 'tomcat',
    group   => 'tomcat',
    mode    => '0640',
    source  => $release_war,
    require => flatten([Package['tomcat10'], $war_requires]),
    notify  => Exec['clear-unpacked-app'],
  }

  # Tomcat unpacks the WAR next to it. Remove the old unpacked copy (and its compiled pages) whenever the WAR
  # changes, so that the new release starts from its own files only, never from a leftover of the previous one.
  exec { 'clear-unpacked-app':
    command     => "rm -rf ${catalina_base}/webapps/grievance-portal ${catalina_base}/work/Catalina/localhost/grievance-portal",
    path        => ['/usr/bin', '/bin'],
    refreshonly => true,
    notify      => Service['tomcat10'],
  }

  service { 'tomcat10':
    ensure  => running,
    enable  => true,
    require => [File[$data_dir], File[$war_target]],
  }

  # --- Health check and the stable marker ------------------------------------------------
  file { $healthcheck:
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0755',
    content => epp('grievance_portal/healthcheck.sh.epp', {
      http_port       => $http_port,
      health_attempts => $health_attempts,
    }),
    require => Package['curl'],
  }

  # Runs only after the WAR or the service changed. If the application does not come up, this exec fails,
  # the run fails, and the release is never marked stable.
  exec { 'health-check':
    command     => $healthcheck,
    path        => ['/usr/bin', '/bin'],
    refreshonly => true,
    timeout     => 300,
    logoutput   => true,
    subscribe   => [File[$war_target], Service['tomcat10']],
    require     => File[$healthcheck],
  }

  # A release becomes the stable one only when it is not recorded yet and the application is healthy now.
  # Once it is recorded this exec is skipped, so a second run changes nothing.
  exec { 'mark-stable':
    command  => "echo '${release}' > ${stable_file}",
    unless   => "grep -qx '${release}' ${stable_file}",
    onlyif   => "${healthcheck} 1",
    path     => ['/usr/bin', '/bin'],
    provider => shell,
    require  => [Exec['health-check'], File[$app_root]],
  }
}
