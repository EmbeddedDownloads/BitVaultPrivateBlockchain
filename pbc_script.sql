-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               5.5.55-0ubuntu0.14.04.1 - (Ubuntu)
-- Server OS:                    debian-linux-gnu
-- HeidiSQL Version:             9.3.0.4984
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping database structure for pbc_db
DROP DATABASE IF EXISTS `pbc_db`;
CREATE DATABASE IF NOT EXISTS `pbc_db` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `pbc_db`;


-- Dumping structure for table pbc_db.block_status
DROP TABLE IF EXISTS `block_status`;
CREATE TABLE IF NOT EXISTS `block_status` (
  `transactionId` varchar(50) NOT NULL,
  `tag` varchar(50) NOT NULL,
  `status` varchar(50) NOT NULL,
  `receiverAddress` varchar(50) DEFAULT NULL,
  `updatedAt` timestamp NULL DEFAULT NULL,
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table pbc_db.block_status: ~8 rows (approximately)
/*!40000 ALTER TABLE `block_status` DISABLE KEYS */;
/*!40000 ALTER TABLE `block_status` ENABLE KEYS */;


-- Dumping structure for table pbc_db.download_url
DROP TABLE IF EXISTS `download_url`;
CREATE TABLE IF NOT EXISTS `download_url` (
  `uuid` varchar(50) NOT NULL,
  `status` tinyint(1) DEFAULT NULL,
  `fileLocation` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table pbc_db.download_url: ~8 rows (approximately)
/*!40000 ALTER TABLE `download_url` DISABLE KEYS */;
/*!40000 ALTER TABLE `download_url` ENABLE KEYS */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
