DROP TABLE IF EXISTS `action_files`;
DROP VIEW IF EXISTS `action_files`;
CREATE TABLE `action_files` (
  `file_id` int(11),
  `action_id` int(11),
  `action_type` varchar(1),
  `commit_id` int(11)
);


DROP TABLE IF EXISTS `actions`;
CREATE TABLE `actions` (
  `id` int(11) NOT NULL DEFAULT '0',
  `type` varchar(1) DEFAULT NULL,
  `file_id` int(11) DEFAULT NULL,
  `commit_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL
);


DROP TABLE IF EXISTS `actions_file_names`;
DROP VIEW IF EXISTS `actions_file_names`;
CREATE TABLE `actions_file_names` (
  `id` int(11),
  `type` varchar(1),
  `file_id` int(11),
  `new_file_name` mediumtext,
  `commit_id` int(11)
);


DROP TABLE IF EXISTS `branches`;
CREATE TABLE `branches` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL
);


DROP TABLE IF EXISTS `content`;
CREATE TABLE `content` (
  `id` int(11) NOT NULL,
  `commit_id` int(11) NOT NULL,
  `file_id` int(11) NOT NULL,
  `content` mediumtext NOT NULL,
  'loc' int(11) NOT NULL
);


DROP TABLE IF EXISTS `file_copies`;
CREATE TABLE `file_copies` (
  `id` int(11) NOT NULL,
  `to_id` int(11) DEFAULT NULL,
  `from_id` int(11) DEFAULT NULL,
  `from_commit_id` int(11) DEFAULT NULL,
  `new_file_name` mediumtext,
  `action_id` int(11) DEFAULT NULL
);


DROP TABLE IF EXISTS `file_links`;
CREATE TABLE `file_links` (
  `id` int(11) NOT NULL,
  `parent_id` int(11) DEFAULT NULL,
  `file_id` int(11) DEFAULT NULL,
  `commit_id` int(11) DEFAULT NULL
);

DROP TABLE IF EXISTS `file_paths`;
CREATE TABLE  `file_paths` (
  `id` int(11) NOT NULL,
  `commit_id` int(11) DEFAULT NULL,
  `file_id` int(11) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL
);

DROP TABLE IF EXISTS `file_types`;
CREATE TABLE `file_types` (
  `id` int(11) NOT NULL,
  `file_id` int(11) DEFAULT NULL,
  `type` mediumtext
);


DROP TABLE IF EXISTS `files`;
CREATE TABLE `files` (
  `id` int(11) NOT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `repository_id` int(11) DEFAULT NULL
);


DROP TABLE IF EXISTS `hunk_blames`;
CREATE TABLE `hunk_blames` (
  `id` int(11) NOT NULL,
  `hunk_id` int(11) DEFAULT NULL,
  `bug_commit_id` mediumtext
);


DROP TABLE IF EXISTS `hunks`;
CREATE TABLE `hunks` (
  `id` int(11) NOT NULL,
  `file_id` int(11) DEFAULT NULL,
  `commit_id` int(11) NOT NULL,
  `old_start_line` int(11) DEFAULT NULL,
  `old_end_line` int(11) DEFAULT NULL,
  `new_start_line` int(11) DEFAULT NULL,
  `new_end_line` int(11) DEFAULT NULL,
  `bug_introducing` tinyint(1) NOT NULL DEFAULT '0'
);

DROP TABLE IF EXISTS `patches`;
CREATE TABLE `patches` (
  `id` int(11) NOT NULL,
  `commit_id` int(11) DEFAULT NULL,
  `patch` longtext
);


DROP TABLE IF EXISTS `people`;
CREATE TABLE `people` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL
);


DROP TABLE IF EXISTS `repositories`;
CREATE TABLE `repositories` (
  `id` int(11) NOT NULL,
  `uri` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(30) DEFAULT NULL
);


DROP TABLE IF EXISTS `scmlog`;
CREATE TABLE `scmlog` (
  `id` int(11) NOT NULL,
  `rev` mediumtext,
  `committer_id` int(11) DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `message` longtext,
  `composed_rev` tinyint(1) DEFAULT NULL,
  `repository_id` int(11) DEFAULT NULL,
  `is_bug_fix` tinyint(1) DEFAULT '0'
);


DROP TABLE IF EXISTS `tag_revisions`;
CREATE TABLE `tag_revisions` (
  `id` int(11) NOT NULL,
  `tag_id` int(11) DEFAULT NULL,
  `commit_id` int(11) DEFAULT NULL
);


DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL
);
