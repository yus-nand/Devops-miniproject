# Entry point for `puppet apply` (see ops/puppet/apply.sh).
# Every setting has a default in the module. The release and the WAR arrive as the facts gp_release and
# war_source; an unset gp_release leaves the module default, an unset war_source means "already on the node".
node default {
  $release = $facts['gp_release'] ? {
    undef   => '1.0.0',
    default => $facts['gp_release'],
  }

  class { 'grievance_portal':
    release    => $release,
    war_source => $facts['war_source'],
  }
}
