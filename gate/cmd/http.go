package main

import (
	"fmt"
	"github.com/ZupIT/charlescd/gate/internal/logging"
	authorizationInteractor "github.com/ZupIT/charlescd/gate/internal/use_case/authorization"
	systemTokenInteractor "github.com/ZupIT/charlescd/gate/internal/use_case/system_token"
	"github.com/ZupIT/charlescd/gate/web/api/handlers"
	"github.com/ZupIT/charlescd/gate/web/api/middlewares"
	"github.com/casbin/casbin/v2"
	"github.com/go-playground/locales/en"
	ut "github.com/go-playground/universal-translator"
	"github.com/go-playground/validator/v10"
	"github.com/go-playground/validator/v10/non-standard/validators"
	enTranslations "github.com/go-playground/validator/v10/translations/en"
	"github.com/labstack/echo-contrib/prometheus"
	"github.com/labstack/echo/v4"
	echoMiddleware "github.com/labstack/echo/v4/middleware"
	"github.com/leebenson/conform"
	"reflect"
	"strings"
)

type server struct {
	persistenceManager persistenceManager
	serviceManager serviceManager
	httpServer     *echo.Echo
	enforcer *casbin.Enforcer
}

type customBinder struct{}

type CustomValidator struct {
	validator *validator.Validate
	translator *ut.UniversalTranslator
}

func newServer(pm persistenceManager, sm serviceManager) (server, error) {
	return server{
		persistenceManager:   pm,
		serviceManager: sm,
		httpServer: createHttpServerInstance(),
	}, nil
}

func (server server) start(port string) error {
	server.registerRoutes()
	return server.httpServer.Start(fmt.Sprintf(":%s", port))
}

func createHttpServerInstance() *echo.Echo {
	httpServer := echo.New()
	httpServer.Use(echoMiddleware.RequestID())
	httpServer.Use(middlewares.ContextLogger)
	httpServer.Validator = buildCustomValidator()
	httpServer.Binder = echo.Binder(customBinder{})

	p := prometheus.NewPrometheus("echo", nil)
	p.Use(httpServer)

	return httpServer
}

func (cb customBinder) Bind(i interface{}, echoCtx echo.Context) (err error) {
	defaultBinder := new(echo.DefaultBinder)
	if err = defaultBinder.Bind(i, echoCtx); err != nil {
		return err
	}

	return conform.Strings(i)
}

func (cv *CustomValidator) Validate(i interface{}) error {
	err := cv.validator.Struct(i)
	if err != nil {
		return logging.NewValidationError(err, cv.translator)
	}
	return nil
}

func buildCustomValidator() *CustomValidator {
	v := validator.New()
	if err := v.RegisterValidation("notblank", validators.NotBlank); err != nil {
		return nil
	}
	v.RegisterTagNameFunc(func(fld reflect.StructField) string {
		name := strings.SplitN(fld.Tag.Get("json"), ",", 2)[0]
		if name == "-" {
			return ""
		}
		return name
	})
	defaultLang := en.New()
	uniTranslator := ut.New(defaultLang, defaultLang)

	defaultTrans, _ := uniTranslator.GetTranslator("en")
	_ = enTranslations.RegisterDefaultTranslations(v, defaultTrans)

	return &CustomValidator{
		validator:  v,
		translator: uniTranslator,
	}
}

func (server server) registerRoutes() {
	server.httpServer.GET("/health", handlers.Health())
	server.httpServer.GET("/metrics", handlers.Metrics())

	api := server.httpServer.Group("/api")
	{
		v1 := api.Group("/v1")
		{
			systemToken := v1.Group("/system-token")
			{
				systemToken.POST("", handlers.CreateSystemToken(systemTokenInteractor.NewCreateSystemToken(server.persistenceManager.systemTokenRepository, server.persistenceManager.permissionRepository, server.persistenceManager.userRepository, server.persistenceManager.workspaceRepository, server.serviceManager.authTokenService)))
				systemToken.GET("/:id", handlers.GetSystemToken(systemTokenInteractor.NewGetSystemToken(server.persistenceManager.systemTokenRepository)))
				systemToken.GET("", handlers.GetAllSystemTokens(systemTokenInteractor.NewGetAllSystemToken(server.persistenceManager.systemTokenRepository)))
				systemToken.POST("/:id/revoke", handlers.RevokeSytemToken(systemTokenInteractor.NewRevokeSystemToken(server.persistenceManager.systemTokenRepository)))
			}

			authorization := v1.Group("/authorization")
			{
				authorization.POST("", handlers.DoAuthorization(authorizationInteractor.NewDoAuthorization(server.enforcer, server.persistenceManager.userRepository, server.serviceManager.authTokenService)))
			}
		}
	}
}
