package rabbitClient

import (
	"gorm.io/gorm"
	"hermes/internal/notification/message"
	"hermes/internal/notification/messageexecutionhistory"
	"hermes/queueprotocol"
)

const (
	enqueued    = "ENQUEUED"
	notEnqueued = "NOT_ENQUEUED"
	delivered = "DELIVERED"
	deliveredFailed = "DELIVERED_FAILED"
	successLog  = "SUCCESS"
)

type UseCases interface {
	Publish(stopPub chan bool) error
	Consume(stopCon chan bool) error
}

type Main struct {
	db            *gorm.DB
	amqpClient    *queueprotocol.Client
	messageMain   message.UseCases
	executionMain messageexecutionhistory.UseCases
}

func NewMain(db *gorm.DB, amqpClient *queueprotocol.Client, messageMain message.UseCases, executionMain messageexecutionhistory.UseCases) UseCases {
	return &Main{db, amqpClient, messageMain, executionMain}
}
