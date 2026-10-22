# Entry point for `puppet apply` (see ops/puppet/apply.sh).
# Every setting has a default in the module; the WAR to deploy arrives as the fact war_source.
node default {
  class { 'grievance_portal':
    war_source => $facts['war_source'],
  }
}
