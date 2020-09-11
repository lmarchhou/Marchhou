/*
Navicat MySQL Data Transfer

Source Server         : 10.3.13.126
Source Server Version : 50520
Source Host           : 10.3.13.126:3306
Source Database       : solar_equipment

Target Server Type    : MYSQL
Target Server Version : 50520
File Encoding         : 65001

Date: 2020-09-01 16:56:10
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for field_numerical_unit
-- ----------------------------
DROP TABLE IF EXISTS `field_numerical_unit`;
CREATE TABLE `field_numerical_unit` (
  `ID` bigint(20) NOT NULL COMMENT 'ID',
  `NAME` varchar(200) DEFAULT NULL COMMENT '单位名称',
  `DELETED` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `CREATE_USER_ID` bigint(20) DEFAULT NULL COMMENT '创建人',
  `CREATE_DATETIME` datetime DEFAULT NULL COMMENT '创建时间',
  `MODIFY_USER_ID` bigint(20) DEFAULT NULL COMMENT '修改人',
  `MODIFY_DATETIME` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数值单位表';

-- ----------------------------
-- Records of field_numerical_unit
-- ----------------------------
INSERT INTO `field_numerical_unit` VALUES ('10001', '次/年', '0', '111111111', '2020-08-12 01:13:13', '111111111', '2020-08-12 01:13:13');
INSERT INTO `field_numerical_unit` VALUES ('10002', '万元', '0', '111111111', '2020-08-12 01:13:13', '111111111', '2020-08-12 01:13:13');
