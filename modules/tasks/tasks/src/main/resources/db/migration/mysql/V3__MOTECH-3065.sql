-- Changing column 'MESSAGE' from VARCHAR(500) to VARCHAR(20000)

alter table MOTECH_TASKS_TASKERROR modify message varchar(20000) NOT NULL;