-- Record the date a student was deactivated (UC-STU-deactivate-student, BR-student-lifecycle).
-- Students and instructors share peer_evaluation_user (single-table inheritance), so the column
-- lives there; only Student maps it, and it stays NULL for instructors and for every active student.
-- A student disabled before this migration also keeps NULL, so she appears in every week's report.

ALTER TABLE `peer_evaluation_user` ADD COLUMN `deactivated_on` date DEFAULT NULL;
