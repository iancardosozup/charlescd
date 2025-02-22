/*
 * Copyright 2020, 2021 ZUP IT SERVICOS EM TECNOLOGIA E INOVACAO SA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.charlescd.moove.api

import io.charlescd.moove.application.ErrorDetailedResponse
import io.charlescd.moove.application.ErrorMessageResponse
import io.charlescd.moove.application.ResourceValueResponse
import io.charlescd.moove.commons.exceptions.*
import io.charlescd.moove.domain.MooveErrorCode
import io.charlescd.moove.domain.exceptions.*
import java.lang.IllegalArgumentException
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.LinkedHashMap
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.support.MissingServletRequestPartException

@ControllerAdvice
class MooveExceptionHandler(private val messageSource: MessageSource) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun exceptions(ex: ConflictException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.CONFLICT, ex.messageDetails)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun exceptions(ex: Exception): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.INTERNAL_SERVER_ERROR, ex.message ?: "Internal Server Error")
    }

    @ExceptionHandler(BadRequestClientException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun clientBadRequestExceptions(ex: BadRequestClientException): ErrorDetailedResponse {
        this.logger.error(ex.details, ex)
        return ErrorDetailedResponse(ex.id, ex.links, ex.title, ex.details, ex.status, ex.source, ex.meta)
    }

    @ExceptionHandler(NotFoundClientException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun clientNotFoundExceptions(ex: NotFoundClientException): ErrorDetailedResponse {
        this.logger.error(ex.details, ex)
        return ErrorDetailedResponse(ex.id, ex.links, ex.title, ex.details, ex.status, ex.source, ex.meta)
    }

    @ExceptionHandler(InternalErrorClientException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun clientInternalErrorExceptions(ex: InternalErrorClientException): ErrorDetailedResponse {
        this.logger.error(ex.details, ex)
        return ErrorDetailedResponse(ex.id, ex.links, ex.title, ex.details, ex.status, ex.source, ex.meta)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleIllegalArgument(ex: IllegalArgumentException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.INVALID_PAYLOAD, ex.message!!)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleArgumentNotValid(ex: MethodArgumentNotValidException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        val fields = LinkedHashMap<String, List<String>>()
        ex.bindingResult.fieldErrors.forEach { field: FieldError ->
            (fields.computeIfAbsent(field.field) { LinkedList() } as LinkedList<String>).add(field.defaultMessage!!)
        }
        return ErrorMessageResponse.of(MooveErrorCode.INVALID_PAYLOAD, fields)
    }

    @ExceptionHandler(DataAccessException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleDataAccessException(ex: DataAccessException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.INTERNAL_SERVER_ERROR, ex.rootCause?.message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleJsonParseError(ex: HttpMessageNotReadableException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.INVALID_PAYLOAD, ex.message!!)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleMissingRequestParametersError(ex: MissingServletRequestParameterException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.MISSING_PARAMETER, ex.parameterName)
    }

    @ExceptionHandler(MissingServletRequestPartException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleMissingServletRequestPartException(ex: MissingServletRequestPartException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.MISSING_PARAMETER, ex.requestPartName)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    fun handleMethodNotAllowedException(request: HttpServletRequest, ex: HttpRequestMethodNotSupportedException) {
        this.logger.error(request.requestURI, ex)
    }

    @ExceptionHandler(BusinessException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    fun businessException(ex: BusinessException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(ex.getErrorCode(),
            messageSource.getMessage(ex.getErrorCode().key, ex.getParameters(), Locale.ENGLISH))
    }

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    fun forbiddenException(request: HttpServletRequest, ex: ForbiddenException): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(MooveErrorCode.FORBIDDEN, ex.message!!)
    }

    @ExceptionHandler(BusinessExceptionLegacy::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    @Deprecated("Only for backwards compatibility")
    fun businessException(ex: BusinessExceptionLegacy): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        return ErrorMessageResponse.of(ex.getErrorCode().name,
            ex.getParameters()
                ?.let { messageSource.getMessage(ex.getErrorCode().key, ex.getParameters(), Locale.ENGLISH) }
                ?: ex.message)
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun handleNotFoundException(request: HttpServletRequest, ex: NotFoundException): ResourceValueResponse {
        this.logger.error(ex.message, ex)
        return ResourceValueResponse(ex.resourceName, ex.id)
    }

    @ExceptionHandler(NotFoundExceptionLegacy::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    @Deprecated("Only for backwards compatibility")
    fun handleNotFoundExceptionLegacy(request: HttpServletRequest, ex: NotFoundExceptionLegacy): ResourceValueResponse {
        this.logger.error(ex.message, ex)
        return ResourceValueResponse(ex.resourceName, ex.id)
    }

    @ExceptionHandler(IntegrationExceptionLegacy::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun integrationException(ex: IntegrationExceptionLegacy): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        val message = messageSource.getMessage(ex.getErrorCode().key, null, Locale.ENGLISH)
        return ErrorMessageResponse.of(ex.getErrorCode().name, message, ex.getDetails())
    }

    @ExceptionHandler(ThirdPartyIntegrationExceptionLegacy::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun integrationException(ex: ThirdPartyIntegrationExceptionLegacy): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        val message = messageSource.getMessage(ex.getErrorCode().key, null, Locale.ENGLISH)
        return ErrorMessageResponse.of(ex.getErrorCode().name, message, ex.getDetails())
    }

    @ExceptionHandler(InvalidRegistryExceptionLegacy::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun invalidIntegrationRequestExceptionLegacy(ex: InvalidRegistryExceptionLegacy): ErrorMessageResponse {
        this.logger.error(ex.message, ex)
        val message = messageSource.getMessage(ex.getErrorCode().key, null, Locale.ENGLISH)
        return ErrorMessageResponse.of(ex.getErrorCode().name, message)
    }
}
