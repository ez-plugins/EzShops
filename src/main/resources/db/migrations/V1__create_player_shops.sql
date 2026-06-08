-- Create player shops table (used by MySQL/Jaloquent backends)
CREATE TABLE IF NOT EXISTS `ez_player_shops` (
  `sign_key` VARCHAR(255) NOT NULL,
  `owner_uuid` VARCHAR(36) NOT NULL,
  `quantity` INT NOT NULL,
  `price` DOUBLE NOT NULL,
  `item_data` MEDIUMTEXT NOT NULL,
  `chests` TEXT NOT NULL,
  PRIMARY KEY (sign_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
