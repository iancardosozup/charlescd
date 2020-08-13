CREATE TABLE IF NOT EXISTS PLUGINS (
	ID VARCHAR(36) PRIMARY KEY,
	NAME VARCHAR(100) NOT NULL,
	SRC VARCHAR(100) NOT NULL,
	CREATED_AT TIMESTAMP DEFAULT clock_timestamp() NOT NULL
);

CREATE TABLE IF NOT EXISTS METRICS_GROUPS (
	ID VARCHAR(36) PRIMARY KEY,
	NAME VARCHAR(100) NOT NULL,
	WORKSPACE_ID VARCHAR(36) NOT NULL,
    STATUS VARCHAR(100) NOT NULL,
	CIRCLE_ID VARCHAR(36) NOT NULL,
	CREATED_AT TIMESTAMP DEFAULT clock_timestamp() NOT NULL
);

CREATE TABLE IF NOT EXISTS DATA_SOURCES (
	ID VARCHAR(36) PRIMARY KEY,
	PLUGIN_ID VARCHAR(50) NOT NULL,
	NAME VARCHAR(100) not null,
	CREATED_AT TIMESTAMP DEFAULT clock_timestamp() NOT NULL,
	DATA JSONB not NULL,
	WORKSPACE_ID VARCHAR(36) NOT NULL,
	HEALTH BOOLEAN NOT NULL DEFAULT FALSE,
    DELETED_AT TIMESTAMP,
	CONSTRAINT fk_metric_plugins FOREIGN KEY(PLUGIN_ID) REFERENCES PLUGINS(id)
);

CREATE TABLE IF NOT exists METRICS (
	ID VARCHAR(36) PRIMARY KEY,
	METRIC VARCHAR(100) NOT NULL,
	THRESHOLD REAL,
	CONDITION VARCHAR(30),
	METRICS_GROUP_ID varchar(36),
	DATA_SOURCE_id VARCHAR(36),
    STATUS VARCHAR(100) NOT NULL,
	CREATED_AT TIMESTAMP DEFAULT clock_timestamp() NOT NULL,
	CONSTRAINT fk_metric_group_metric FOREIGN KEY(METRICS_GROUP_ID) REFERENCES METRICS_GROUPS(id),
	CONSTRAINT fk_metric_data_sources FOREIGN KEY(DATA_SOURCE_id) REFERENCES DATA_SOURCES(id)
);

CREATE TABLE IF NOT EXISTS METRIC_FILTERS (
	ID VARCHAR(36) PRIMARY KEY,
	FIELD VARCHAR(100) NOT NULL,
	VALUE VARCHAR(100) NOT NULL,
	OPERATOR VARCHAR(30) not null,
	METRIC_ID varchar(36),
	CREATED_AT TIMESTAMP DEFAULT clock_timestamp() NOT NULL,
	CONSTRAINT fk_metric_metric_filter FOREIGN KEY(METRIC_ID) REFERENCES METRICS(ID)
);

CREATE TABLE IF NOT EXISTS METRIC_GROUP_BIES (
	ID VARCHAR(36) PRIMARY KEY,
	FIELD VARCHAR(100) NOT NULL,
	METRIC_ID varchar(36),
	CREATED_AT TIMESTAMP DEFAULT clock_timestamp() NOT NULL,
	CONSTRAINT fk_metric_metric_group_by FOREIGN KEY(metric_id) REFERENCES METRICS(id)
);
