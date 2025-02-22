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

package io.charlescd.moove.application.circle.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.charlescd.moove.application.*
import io.charlescd.moove.application.circle.CreateCircleWithCsvFileInteractor
import io.charlescd.moove.application.circle.request.CreateCircleWithCsvRequest
import io.charlescd.moove.application.circle.request.NodePart
import io.charlescd.moove.domain.Circle
import io.charlescd.moove.domain.MatcherTypeEnum
import io.charlescd.moove.domain.exceptions.BusinessException
import io.charlescd.moove.domain.repository.CircleRepository
import io.charlescd.moove.domain.repository.KeyValueRuleRepository
import io.charlescd.moove.domain.repository.SystemTokenRepository
import io.charlescd.moove.domain.repository.UserRepository
import io.charlescd.moove.domain.repository.WorkspaceRepository
import io.charlescd.moove.domain.service.CircleMatcherService
import io.charlescd.moove.domain.service.ManagementUserSecurityService
import spock.lang.Specification

class CreateCircleWithCsvFileInteractorImplTest extends Specification {

    private CreateCircleWithCsvFileInteractor createCircleWithCsvFileInteractor

    private UserRepository userRepository = Mock(UserRepository)
    private CircleRepository circleRepository = Mock(CircleRepository)
    private SystemTokenRepository systemTokenRepository = Mock(SystemTokenRepository)
    private CircleMatcherService circleMatcherService = Mock(CircleMatcherService)
    private KeyValueRuleRepository keyValueRuleRepository = Mock(KeyValueRuleRepository)
    private WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new KotlinModule()).registerModule(new JavaTimeModule())
    private CsvSegmentationService csvSegmentationService = new CsvSegmentationService(objectMapper)
    private SystemTokenService systemTokenService = new SystemTokenService(systemTokenRepository)
    private ManagementUserSecurityService managementUserSecurityService = Mock(ManagementUserSecurityService)

    void setup() {
        this.createCircleWithCsvFileInteractor = new CreateCircleWithCsvFileInteractorImpl(
                new UserService(userRepository, systemTokenService, managementUserSecurityService),
                new CircleService(circleRepository),
                circleMatcherService,
                new KeyValueRuleService(keyValueRuleRepository),
                csvSegmentationService,
                new WorkspaceService(workspaceRepository, userRepository)
        )
    }

    def "should create a new circle with a csv file and return four rules on preview using authorization"() {
        given:
        def fileContent = "IDs\n" +
                "ce532f07-3bcf-40f8-9a39-289fb527ed54\n" +
                "c4b13c9f-d151-4f68-aad5-313b08503bd6\n" +
                "d77c5d16-a39f-406e-a33b-cee986b82348\n" +
                "2dd5fd08-c23a-494a-80b6-66db39c73630\n"

        def inputStream = new ByteArrayInputStream(fileContent.getBytes())

        def name = "Women"
        def keyName = "IDs"


        def author = TestUtils.user
        def workspace = TestUtils.workspace
        def authorId = TestUtils.authorId
        def workspaceId = TestUtils.workspaceId
        def authorization = TestUtils.authorization

        def request = new CreateCircleWithCsvRequest(name, keyName, inputStream)

        when:
        def response = this.createCircleWithCsvFileInteractor.execute(request, workspaceId, authorization, null)

        then:
        1 * managementUserSecurityService.getUserEmail(authorization) >> author.email
        1 * userRepository.findByEmail(author.email) >> Optional.of(author)
        1 * this.circleRepository.save(_) >> { arguments ->
            def circle = arguments[0]

            assert circle instanceof Circle
            assert circle.id != null
            assert circle.name == name
            assert circle.reference != null
            assert !circle.defaultCircle
            assert circle.author == author
            assert circle.createdAt != null
            assert circle.importedAt != null
            assert circle.matcherType == MatcherTypeEnum.SIMPLE_KV
            assert circle.workspaceId == workspaceId

            return circle
        }

        1 * this.circleRepository.update(_) >> { arguments ->
            def circle = arguments[0]

            assert circle instanceof Circle
            assert circle.id != null
            assert circle.name == name
            assert circle.reference != null
            assert !circle.defaultCircle
            assert circle.author == author
            assert circle.createdAt != null
            assert circle.importedAt != null
            assert circle.rules != null
            assert circle.importedKvRecords == 4
            assert circle.matcherType == MatcherTypeEnum.SIMPLE_KV
            assert circle.workspaceId == workspaceId

            return circle
        }

        1 * this.workspaceRepository.find(workspaceId) >> Optional.of(workspace)

        1 * this.circleMatcherService.createImport(_, _, _, _) >> { arguments ->
            def circle = arguments[0]
            def nodes = arguments[1]
            def matcherUri = arguments[2]

            assert circle instanceof Circle
            assert nodes instanceof List<JsonNode>
            assert matcherUri == workspace.circleMatcherUrl
        }

        assert response != null
        assert response.id != null
        assert response.name == name
        assert response.importedKvRecords == 4
        assert response.workspaceId == workspaceId
        assert response.reference != null
        assert response.createdAt != null
        assert response.importedAt != null
        assert response.author.id == authorId
        assert response.matcherType == MatcherTypeEnum.SIMPLE_KV
        assert !response.default
        assert response.rules != null

        def rules = objectMapper.treeToValue(response.rules, NodePart.class)

        assert rules.type == NodePart.NodeTypeRequest.CLAUSE
        assert rules.clauses.size() == 1
        assert rules.clauses[0].clauses.size() == 4
    }

    def "should create a new circle with a csv file and return four rules on preview using system token"() {
        given:
        def fileContent = "IDs\n" +
                "ce532f07-3bcf-40f8-9a39-289fb527ed54\n" +
                "c4b13c9f-d151-4f68-aad5-313b08503bd6\n" +
                "d77c5d16-a39f-406e-a33b-cee986b82348\n" +
                "2dd5fd08-c23a-494a-80b6-66db39c73630\n"

        def inputStream = new ByteArrayInputStream(fileContent.getBytes())

        def name = "Women"
        def keyName = "IDs"


        def author = TestUtils.user
        def workspace = TestUtils.workspace
        def authorId = TestUtils.authorId
        def workspaceId = TestUtils.workspaceId
        def systemTokenValue = TestUtils.systemTokenValue
        def systemTokenId = TestUtils.systemTokenId

        def request = new CreateCircleWithCsvRequest(name, keyName, inputStream)

        when:
        def response = this.createCircleWithCsvFileInteractor.execute(request, workspaceId, null, systemTokenValue)

        then:
        1 * systemTokenRepository.getIdByTokenValue(systemTokenValue) >> systemTokenId
        1 * userRepository.findBySystemTokenId(systemTokenId) >> Optional.of(author)
        1 * this.circleRepository.save(_) >> { arguments ->
            def circle = arguments[0]

            assert circle instanceof Circle
            assert circle.id != null
            assert circle.name == name
            assert circle.reference != null
            assert !circle.defaultCircle
            assert circle.author == author
            assert circle.createdAt != null
            assert circle.importedAt != null
            assert circle.matcherType == MatcherTypeEnum.SIMPLE_KV
            assert circle.workspaceId == workspaceId

            return circle
        }

        1 * this.circleRepository.update(_) >> { arguments ->
            def circle = arguments[0]

            assert circle instanceof Circle
            assert circle.id != null
            assert circle.name == name
            assert circle.reference != null
            assert !circle.defaultCircle
            assert circle.author == author
            assert circle.createdAt != null
            assert circle.importedAt != null
            assert circle.rules != null
            assert circle.importedKvRecords == 4
            assert circle.matcherType == MatcherTypeEnum.SIMPLE_KV
            assert circle.workspaceId == workspaceId

            return circle
        }

        1 * this.workspaceRepository.find(workspaceId) >> Optional.of(workspace)

        1 * this.circleMatcherService.createImport(_, _, _, _) >> { arguments ->
            def circle = arguments[0]
            def nodes = arguments[1]
            def matcherUri = arguments[2]

            assert circle instanceof Circle
            assert nodes instanceof List<JsonNode>
            assert matcherUri == workspace.circleMatcherUrl
        }

        assert response != null
        assert response.id != null
        assert response.name == name
        assert response.importedKvRecords == 4
        assert response.workspaceId == workspaceId
        assert response.reference != null
        assert response.createdAt != null
        assert response.importedAt != null
        assert response.author.id == authorId
        assert response.matcherType == MatcherTypeEnum.SIMPLE_KV
        assert !response.default
        assert response.rules != null

        def rules = objectMapper.treeToValue(response.rules, NodePart.class)

        assert rules.type == NodePart.NodeTypeRequest.CLAUSE
        assert rules.clauses.size() == 1
        assert rules.clauses[0].clauses.size() == 4
    }

    def "should throw BusinessException when matcher url is missing on workspace"() {
        given:
        def fileContent = "IDs\n" +
                "ce532f07-3bcf-40f8-9a39-289fb527ed54\n" +
                "c4b13c9f-d151-4f68-aad5-313b08503bd6\n" +
                "d77c5d16-a39f-406e-a33b-cee986b82348\n" +
                "2dd5fd08-c23a-494a-80b6-66db39c73630\n"

        def inputStream = new ByteArrayInputStream(fileContent.getBytes())

        def name = "Women"
        def keyName = "IDs"


        def author = TestUtils.user
        def workspaceId = TestUtils.workspaceId
        def systemTokenValue = TestUtils.systemTokenValue
        def systemTokenId = TestUtils.systemTokenId
        def circle = TestUtils.circle
        def request = new CreateCircleWithCsvRequest(name, keyName, inputStream)

        when:
        this.createCircleWithCsvFileInteractor.execute(request, workspaceId, null, systemTokenValue)

        then:
        1 * systemTokenRepository.getIdByTokenValue(systemTokenValue) >> systemTokenId
        1 * userRepository.findBySystemTokenId(systemTokenId) >> Optional.of(author)
        1 * this.circleRepository.save(_) >> circle

        1 * this.circleRepository.update(_) >> circle

        1 * this.workspaceRepository.find(workspaceId) >> Optional.of(TestUtils.workspaceWithoutMatcher)

        def exception = thrown(BusinessException)
        assert exception.message == "workspace.matcher_url.is.missing"
    }

    def "should create a new circle with a csv file and return five rules on preview"() {
        given:
        def fileContent = "IDs\n" +
                "ce532f07-3bcf-40f8-9a39-289fb527ed54\n" +
                "c4b13c9f-d151-4f68-aad5-313b08503bd6\n" +
                "d77c5d16-a39f-406e-a33b-cee986b82348\n" +
                "2dd5fd08-c23a-494a-80b6-66db39c73630\n" +
                "14034c1e-7429-4835-8ba3-e836bd2e8bc4\n" +
                "c7819b88-55a6-458b-bebf-96af0e26ed2f"

        def inputStream = new ByteArrayInputStream(fileContent.getBytes())

        def name = "Women"
        def workspaceId = TestUtils.workspaceId
        def keyName = "IDs"
        def authorId = TestUtils.authorId
        def author = TestUtils.user
        def workspace = TestUtils.workspace
        def authorization = TestUtils.authorization

        def request = new CreateCircleWithCsvRequest(name, keyName, inputStream)

        when:
        def response = this.createCircleWithCsvFileInteractor.execute(request, workspaceId, authorization, null)

        then:
        1 * managementUserSecurityService.getUserEmail(authorization) >> author.email
        1 * userRepository.findByEmail(author.email) >> Optional.of(author)
        1 * this.circleRepository.save(_) >> { arguments ->
            def circle = arguments[0]

            assert circle instanceof Circle
            assert circle.id != null
            assert circle.name == name
            assert circle.reference != null
            assert !circle.defaultCircle
            assert circle.author == author
            assert circle.createdAt != null
            assert circle.importedAt != null
            assert circle.matcherType == MatcherTypeEnum.SIMPLE_KV
            assert circle.workspaceId == workspaceId

            return circle
        }

        1 * this.circleRepository.update(_) >> { arguments ->
            def circle = arguments[0]

            assert circle instanceof Circle
            assert circle.id != null
            assert circle.name == name
            assert circle.reference != null
            assert !circle.defaultCircle
            assert circle.author == author
            assert circle.createdAt != null
            assert circle.importedAt != null
            assert circle.rules != null
            assert circle.importedKvRecords == 6
            assert circle.matcherType == MatcherTypeEnum.SIMPLE_KV
            assert circle.workspaceId == workspaceId

            return circle
        }

        1 * this.workspaceRepository.find(workspaceId) >> Optional.of(workspace)

        1 * this.circleMatcherService.createImport(_, _, _, _) >> { arguments ->
            def circle = arguments[0]
            def nodes = arguments[1]
            def matcherUri = arguments[2]

            assert circle instanceof Circle
            assert nodes instanceof List<JsonNode>
            assert matcherUri == workspace.circleMatcherUrl
        }

        assert response != null
        assert response.id != null
        assert response.name == name
        assert response.importedKvRecords == 6 //current imported records
        assert response.workspaceId == workspaceId
        assert response.reference != null
        assert response.createdAt != null
        assert response.importedAt != null
        assert response.author.id == authorId
        assert response.matcherType == MatcherTypeEnum.SIMPLE_KV
        assert !response.default
        assert response.rules != null

        def rules = objectMapper.treeToValue(response.rules, NodePart.class)

        assert rules.type == NodePart.NodeTypeRequest.CLAUSE
        assert rules.clauses.size() == 1
        assert rules.clauses[0].clauses.size() == 5 //preview
    }
}
