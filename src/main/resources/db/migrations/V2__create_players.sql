-- Create players table used for linking transactions and shops
CREATE TABLE IF NOT EXISTS `ez_players` (
  `uuid` VARCHAR(36) PRIMARY KEY,
  `first_seen` BIGINT,
  `last_seen` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
