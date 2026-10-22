# @summary Provisions Tomcat 10 on Ubuntu 24.04 and deploys the Policyholder Grievance Portal WAR.
#
# The specification this class implements is docs/07-server-specification.md.
#
# @param war_source   Path of the grievance-portal.war to deploy (required).
# @param http_port    Port Tomcat listens on.
# @param env_name     Label shown in the page footer and in /health (GP_ENV_NAME).
# @param java_package Package that provides the Java runtime.
# @param java_home    JAVA_HOME that belongs to java_package.
# @param data_dir     Folder for the H2 database files (GP_DB_PATH is set to a file inside it).
# @param heap_min     JVM -Xms.
# @param heap_max     JVM -Xmx.
class grievance_portal (
  Optional[String]    $war_source   = undef,
  Integer[1024,65535] $http_port    = 8083,
  String              $env_name     = 'puppet-node',
  String              $java_package = 'openjdk-17-jre-headless',
  String              $java_home    = '/usr/lib/jvm/java-17-openjdk-amd64',
  String              $data_dir     = '/var/lib/grievance-portal',
  String              $heap_min     = '128m',
  String              $heap_max     = '512m',
) {
  if $war_source == undef {
    fail('grievance_portal: war_source is not set. Use ops/puppet/apply.sh, or set FACTER_war_source=/path/to/grievance-portal.war')
  }

  $catalina_base = '/var/lib/tomcat10'
  $dropin_dir    = '/etc/systemd/system/tomcat10.service.d'
  $war_target    = "${catalina_base}/webapps/grievance-portal.war"

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

  # --- The application -------------------------------------------------------------------
  # No notify here on purpose: Tomcat redeploys a changed WAR by itself, so a new release needs no restart.
  file { $war_target:
    ensure  => file,
    owner   => 'tomcat',
    group   => 'tomcat',
    mode    => '0640',
    source  => $war_source,
    require => Package['tomcat10'],
  }

  # --- Service ---------------------------------------------------------------------------
  service { 'tomcat10':
    ensure  => running,
    enable  => true,
    require => [File[$data_dir], File[$war_target]],
  }
}
