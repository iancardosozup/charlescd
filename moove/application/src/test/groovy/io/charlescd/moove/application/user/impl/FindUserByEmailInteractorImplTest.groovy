/*
 * Copyright 2020 ZUP IT SERVICOS EM TECNOLOGIA E INOVACAO SA
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

package io.charlescd.moove.application.user.impl

import io.charlescd.moove.application.SystemTokenService
import io.charlescd.moove.application.TestUtils
import io.charlescd.moove.application.UserService
import io.charlescd.moove.application.user.FindUserByEmailInteractor
import io.charlescd.moove.domain.Permission
import io.charlescd.moove.domain.User
import io.charlescd.moove.domain.WorkspacePermissions
import io.charlescd.moove.domain.WorkspaceStatusEnum
import io.charlescd.moove.domain.exceptions.ForbiddenException
import io.charlescd.moove.domain.repository.SystemTokenRepository
import io.charlescd.moove.domain.repository.UserRepository
import io.charlescd.moove.domain.service.ManagementUserSecurityService
import spock.lang.Specification

import java.time.LocalDateTime

class FindUserByEmailInteractorImplTest extends Specification {

    private FindUserByEmailInteractor findUserByEmailInteractor

    private UserRepository userRepository = Mock(UserRepository)
    private SystemTokenService systemTokenService = new SystemTokenService(Mock(SystemTokenRepository))
    private ManagementUserSecurityService managementUserSecurityService = Mock(ManagementUserSecurityService)

    void setup() {
        findUserByEmailInteractor = new FindUserByEmailInteractorImpl(new UserService(userRepository, systemTokenService, managementUserSecurityService))
    }

    def "should find an user by its email"() {
        given:
        def base64Email = "dXNlckB6dXAuY29tLmJy"

        def author = new User("f52f94b8-6775-470f-bac8-125ebfd6b636", "zup", "zup@zup.com.br", "http://image.com.br/photo.png",
                [], [], false, LocalDateTime.now())

        def permission = new Permission("permission-id", "permission-name", LocalDateTime.now())
        def workspacePermission = new WorkspacePermissions("workspace-id", "workspace-name", [permission], author, LocalDateTime.now(), WorkspaceStatusEnum.COMPLETE)

        def user = new User("cfb1a3a4-d3af-46c6-b6c3-33f30f68b28b", "user name", "user@zup.com.br", "http://image.com.br/photo.png",
                [], [workspacePermission], false, LocalDateTime.now())

        def authorization = TestUtils.authorization

        when:
        def response = findUserByEmailInteractor.execute(base64Email, authorization)

        then:
        1 * this.managementUserSecurityService.getUserEmail(authorization) >> "email@email"
        1 * this.userRepository.findByEmail("email@email") >> Optional.of(TestUtils.userRoot)
        1 * userRepository.findByEmail("user@zup.com.br") >> Optional.of(user)

        assert response != null
        assert response.id == user.id
        assert response.name == user.name
        assert response.createdAt == user.createdAt
        assert response.photoUrl == user.photoUrl
    }

    def "should find an user by email when requester is root"() {
        given:
        def base64Email = "dXNlckB6dXAuY29tLmJy"

        def author = new User("f52f94b8-6775-470f-bac8-125ebfd6b636", "zup", "zup@zup.com.br", "http://image.com.br/photo.png",
                [], [], true, LocalDateTime.now())

        def permission = new Permission("permission-id", "permission-name", LocalDateTime.now())
        def workspacePermission = new WorkspacePermissions("workspace-id", "workspace-name", [permission], author, LocalDateTime.now(), WorkspaceStatusEnum.COMPLETE)

        def user = new User("cfb1a3a4-d3af-46c6-b6c3-33f30f68b28b", "user name", "user@zup.com.br", "http://image.com.br/photo.png",
                [], [workspacePermission], false, LocalDateTime.now())

        def authorization = TestUtils.authorization

        when:
        def response = findUserByEmailInteractor.execute(base64Email, authorization)

        then:
        1 * this.managementUserSecurityService.getUserEmail(authorization) >> "email@email"
        1 * this.userRepository.findByEmail("email@email") >> Optional.of(TestUtils.userRoot)
        1 * this.userRepository.findByEmail("user@zup.com.br") >> Optional.of(user)

        assert response != null
        assert response.id == user.id
        assert response.name == user.name
        assert response.createdAt == user.createdAt
        assert response.photoUrl == user.photoUrl
    }

    def "should return user by email when requester is not root but is the user himself"() {
        given:
        def base64Email = "dXNlckB6dXAuY29tLmJy"

        def author = new User("f52f94b8-6775-470f-bac8-125ebfd6b636", "zup", "zup@zup.com.br", "http://image.com.br/photo.png",
                [], [], false, LocalDateTime.now())

        def permission = new Permission("permission-id", "permission-name", LocalDateTime.now())
        def workspacePermission = new WorkspacePermissions("workspace-id", "workspace-name", [permission], author, LocalDateTime.now(), WorkspaceStatusEnum.COMPLETE)

        def user = new User("cfb1a3a4-d3af-46c6-b6c3-33f30f68b28b", "user name", "user@zup.com.br", "http://image.com.br/photo.png",
                [], [workspacePermission], false, LocalDateTime.now())

        def authorization = TestUtils.authorization

        when:
        def response = findUserByEmailInteractor.execute(base64Email, authorization)

        then:
        1 * this.managementUserSecurityService.getUserEmail(authorization) >> "user@zup.com.br"
        1 * this.userRepository.findByEmail("user@zup.com.br") >> Optional.of(user)

        assert response != null
        assert response.id == user.id
        assert response.name == user.name
        assert response.createdAt == user.createdAt
        assert response.photoUrl == user.photoUrl
    }

    def "when requester is not root and not the user himself should throw ForbiddenException"() {
        given:
        def base64Email = "dXNlckB6dXAuY29tLmJy"

        def author = new User("f52f94b8-6775-470f-bac8-125ebfd6b636", "zup", "zup@zup.com.br", "http://image.com.br/photo.png",
                [], [], false, LocalDateTime.now())

        def permission = new Permission("permission-id", "permission-name", LocalDateTime.now())
        def workspacePermission = new WorkspacePermissions("workspace-id", "workspace-name", [permission], author, LocalDateTime.now(), WorkspaceStatusEnum.COMPLETE)

        def user = new User("cfb1a3a4-d3af-46c6-b6c3-33f30f68b28b", "user name", "user@zup.com.br", "http://image.com.br/photo.png",
                [], [workspacePermission], false, LocalDateTime.now())

        def authorization = TestUtils.authorization

        when:
        findUserByEmailInteractor.execute(base64Email, authorization)

        then:
        1 * this.managementUserSecurityService.getUserEmail(authorization) >> "email@email"
        1 * this.userRepository.findByEmail("email@email") >> Optional.of(TestUtils.user)
        0 * this.userRepository.findByEmail(TestUtils.user.email) >> Optional.of(user)

        thrown(ForbiddenException)
    }

    def "should return an user with an empty workspace when no workspaces where found"() {
        given:
        def base64Email = "dXNlckB6dXAuY29tLmJy"

        def user = new User("cfb1a3a4-d3af-46c6-b6c3-33f30f68b28b", "user name", "user@zup.com.br", "http://image.com.br/photo.png",
                [], [], false, LocalDateTime.now())

        def authorization = TestUtils.authorization

        when:
        def response = findUserByEmailInteractor.execute(base64Email, authorization)

        then:
        1 * this.managementUserSecurityService.getUserEmail(authorization) >> "email@email"
        1 * this.userRepository.findByEmail("email@email") >> Optional.of(TestUtils.userRoot)
        1 * userRepository.findByEmail("user@zup.com.br") >> Optional.of(user)

        assert response != null
        assert response.id == user.id
        assert response.name == user.name
        assert response.createdAt == user.createdAt
        assert response.photoUrl == user.photoUrl
    }
}
