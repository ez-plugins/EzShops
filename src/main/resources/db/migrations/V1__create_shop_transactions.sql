-- Create shop transactions table used by transaction persistence
CREATE TABLE IF NOT EXISTS `ez_shop_transactions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `occurred_at` BIGINT NOT NULL,
  `type` VARCHAR(10) NOT NULL,
  `player_uuid` VARCHAR(36),
  `item_yaml` MEDIUMTEXT,
  `quantity` INT NOT NULL,
  `total` DOUBLE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
