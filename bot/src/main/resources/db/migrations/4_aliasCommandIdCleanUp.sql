update CHANNEL_CONFIG
set COMMAND_ID = 'channel_config'
where COMMAND_ID = 'r'
  and CONFIG_CLASS_ID in ('AliasConfig', 'UserAliasConfig');