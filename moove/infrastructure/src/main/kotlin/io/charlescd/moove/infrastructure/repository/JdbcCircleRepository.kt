/*
 *
 *  * Copyright 2020 ZUP IT SERVICOS EM TECNOLOGIA E INOVACAO SA
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.charlescd.moove.infrastructure.repository

import io.charlescd.moove.domain.*
import io.charlescd.moove.domain.repository.CircleRepository
import io.charlescd.moove.infrastructure.repository.mapper.CircleExtractor
import io.charlescd.moove.infrastructure.repository.mapper.CircleHistoryExtractor
import io.charlescd.moove.infrastructure.repository.mapper.CircleMetricExtractor
import io.charlescd.moove.infrastructure.repository.mapper.CircleSimpleExtractor
import java.sql.Types
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JdbcCircleRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val circleExtractor: CircleExtractor,
    private val circleSimpleExtractor: CircleSimpleExtractor,
    private val circleMetricExtractor: CircleMetricExtractor,
    private val circleHistoryExtractor: CircleHistoryExtractor
) : CircleRepository {

    companion object {
        const val BASE_QUERY_STATEMENT = """
                SELECT DISTINCT circles.id         AS circle_id,
                       circles.name                AS circle_name,
                       circles.reference           AS circle_reference,
                       circles.created_at          AS circle_created_at,
                       circles.matcher_type        AS circle_matcher_type,
                       circles.rules               AS circle_rules,
                       circles.imported_kv_records AS circle_imported_kv_records,
                       circles.imported_at         AS circle_imported_at,
                       circles.default_circle      AS circle_default,
                       circles.workspace_id        AS circle_workspace_id,
                       circles.percentage          AS circle_percentage,
                       circle_user.id              AS circle_user_id,
                       circle_user.name            AS circle_user_name,
                       circle_user.email           AS circle_user_email,
                       circle_user.photo_url       AS circle_user_photo_url,
                       circle_user.created_at      AS circle_user_created_at,
                       CASE 
                        WHEN (deployments.status NOT IN ('NOT_DEPLOYED', 'DEPLOY_FAILED')) THEN TRUE 
                        ELSE FALSE 
                       END AS circle_active
                FROM circles
                         LEFT JOIN users circle_user ON circles.user_id = circle_user.id
                         LEFT JOIN deployments ON circles.id = deployments.circle_id
                WHERE 1 = 1
              """
    }

    override fun save(circle: Circle): Circle {
        createCircle(circle)
        return findById(circle.id).get()
    }

    override fun findById(id: String): Optional<Circle> {
        return findCircleById(id)
    }

    override fun findByIdAndWorkspaceId(id: String, workspaceId: String): Optional<Circle> {
        return findCircleByIdAndWorkspaceId(id, workspaceId)
    }

    override fun find(name: String?, active: Boolean?, workspaceId: String, pageRequest: PageRequest): Page<Circle> {
        val result = this.jdbcTemplate.query(
            createQueryLimit(name, active),
            createParametersArray(name, active, workspaceId, pageRequest),
            circleExtractor
        )

        return Page(
            result?.toList() ?: emptyList(),
            pageRequest.page,
            pageRequest.size,
            executeCountQuery(name, active, workspaceId) ?: 0
        )
    }

    override fun find(name: String?, except: String?, status: Boolean?, workspaceId: String, pageRequest: PageRequest): Page<SimpleCircle> {
        val result = this.jdbcTemplate.query(
            createQueryLimit(name, except, status),
            createParametersSimpleCircleArray(name, except, workspaceId, pageRequest),
            circleSimpleExtractor
        )

        return Page(
            result?.toList() ?: emptyList(),
            pageRequest.page,
            pageRequest.size,
            executeSimpleCircleCountQuery(name, status, except, workspaceId) ?: 0
        )
    }

    private fun createQueryLimit(name: String?, active: Boolean?): String {
        val query = createQuery(name, active)
        query.appendln("LIMIT ?")
            .appendln("OFFSET ?")

        return query.toString()
    }

    private fun createQueryLimit(name: String?, except: String?, status: Boolean?): String {
        val query = createQuery(name, except, status)
        query.appendln("LIMIT ?")
            .appendln("OFFSET ?")

        return query.toString()
    }

    override fun findDefaultByWorkspaceId(workspaceId: String): Optional<Circle> {
        val statement = StringBuilder(BASE_QUERY_STATEMENT)
            .appendln("AND circles.workspace_id = ?")
            .appendln("AND circles.default_circle = ?")

        return Optional.ofNullable(
            this.jdbcTemplate.query(statement.toString(), arrayOf(workspaceId, true), circleExtractor)?.firstOrNull()
        )
    }

    override fun update(circle: Circle): Circle {
        return updateCircle(circle)
    }

    override fun delete(id: String) {
        deleteCircleById(id)
    }

    override fun existsByNameAndWorkspaceId(name: String, workspaceId: String): Boolean {
        val baseCountQuery = """
                SELECT count(*) AS total
                FROM circles 
                WHERE 1 = 1
                """
        val countStatement = StringBuilder(baseCountQuery)
            .appendln("AND circles.workspace_id = ?")
            .appendln("AND circles.name = ?")
            .toString()
        return applyCountQuery(
            countStatement, arrayOf(workspaceId, name)
        )
    }

    private fun applyCountQuery(statement: String, params: Array<String>): Boolean {
        val count = this.jdbcTemplate.queryForObject(
            statement,
            params
        ) { rs, _ -> rs.getInt(1) }
        return count != null && count >= 1
    }

    private fun createParametersArray(name: String?, active: Boolean?, workspaceId: String, pageRequest: PageRequest? = null): Array<Any> {
        val parameters = ArrayList<Any>()
        if (active != null && !active) parameters.add(workspaceId)
        name?.let { parameters.add("%$name%") }
        parameters.add(workspaceId)
        pageRequest?.let {
            parameters.add(pageRequest.size)
            parameters.add(pageRequest.offset())
        }
        return parameters.toTypedArray()
    }

    private fun createParametersSimpleCircleArray(name: String?, except: String?, workspaceId: String, pageRequest: PageRequest? = null): Array<Any> {
        val parameters = ArrayList<Any>()
        name?.let { parameters.add("%$name%") }
        except?.let { parameters.add(except) }
        parameters.add(workspaceId)
        pageRequest?.let {
            parameters.add(pageRequest.size)
            parameters.add(pageRequest.offset())
        }
        return parameters.toTypedArray()
    }

    private fun executeCountQuery(name: String?, active: Boolean?, workspaceId: String): Int? {
        if (active != null) {
            return when (active) {
                true -> executeActiveCircleCountQuery(name, workspaceId)
                else -> executeInactiveCircleCountQuery(name, workspaceId)
            }
        }

        return executeCircleCountQuery(name, workspaceId)
    }

    private fun executeSimpleCircleCountQuery(name: String?, status: Boolean?, except: String?, workspaceId: String): Int? {
        return when (status) {
            true -> executeCircleSimpleCountQuery(name, except, workspaceId)
            else -> executeCircleSimpleActiveCountQuery(name, except, workspaceId)
        }
    }

    private fun createQuery(name: String?, active: Boolean?): StringBuilder {
        if (active == null) {
            return createCircleQuery(name)
        }

        return when (active) {
            true -> createActiveCircleQuery(name)
            else -> createInactiveCircleQuery(name)
        }
    }

    private fun createQuery(name: String?, except: String?, status: Boolean?): StringBuilder {
        return when (status) {
            true -> createSimpleCircleActiveQuery(name, except)
            else -> createSimpleCircleQuery(name, except)
        }
    }

    private fun deleteCircleById(id: String) {
        val statement = "DELETE FROM circles WHERE id = ?"

        this.jdbcTemplate.update(statement, id)
    }

    private fun createCircle(circle: Circle) {
        val statement = StringBuilder(
            """
               INSERT INTO circles(id,
                        name,
                        reference,
                        created_at,
                        matcher_type,
                        rules,
                        imported_at,
                        imported_kv_records,
                        user_id,
                        default_circle,
                        workspace_id,
                        percentage)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
        )

        this.jdbcTemplate.update(statement.toString()) { ps ->
            ps.setString(1, circle.id)
            ps.setString(2, circle.name)
            ps.setString(3, circle.reference)
            ps.setObject(4, circle.createdAt)
            ps.setString(5, circle.matcherType.name)
            ps.setObject(6, circle.rules, Types.OTHER)
            ps.setObject(7, circle.importedAt)
            ps.setObject(8, circle.importedKvRecords)
            ps.setString(9, circle.author?.id)
            ps.setBoolean(10, circle.defaultCircle)
            ps.setString(11, circle.workspaceId)
            ps.setObject(12, circle.percentage)
        }
    }

    private fun findCircleById(id: String): Optional<Circle> {
        val statement = StringBuilder(BASE_QUERY_STATEMENT)
            .appendln("AND circles.id = ?")

        return Optional.ofNullable(
            this.jdbcTemplate.query(statement.toString(), arrayOf(id), circleExtractor)?.firstOrNull()
        )
    }

    private fun findCircleByIdAndWorkspaceId(id: String, workspaceId: String): Optional<Circle> {
        val statement = StringBuilder(BASE_QUERY_STATEMENT)
            .appendln("AND circles.id = ? ")
            .appendln("AND circles.workspace_id = ?")

        return Optional.ofNullable(
            this.jdbcTemplate.query(statement.toString(), arrayOf(id, workspaceId), circleExtractor)?.firstOrNull()
        )
    }

    private fun updateCircle(circle: Circle): Circle {
        val statement = StringBuilder(
            """
                    UPDATE circles
                    SET name                = ?,
                        reference           = ?,
                        matcher_type        = ?,
                        rules               = ?,
                        imported_at         = ?,
                        imported_kv_records = ?,
                        percentage          = ?
                    WHERE id = ?
                """
        )

        this.jdbcTemplate.update(statement.toString()) { ps ->
            ps.setString(1, circle.name)
            ps.setString(2, circle.reference)
            ps.setString(3, circle.matcherType.name)
            ps.setObject(4, circle.rules, Types.OTHER)
            ps.setObject(5, circle.importedAt)
            ps.setObject(6, circle.importedKvRecords)
            ps.setObject(7, circle.percentage)
            ps.setObject(8, circle.id)
        }

        return findById(circle.id).get()
    }

    private fun executeCircleCountQuery(name: String?, workspaceId: String): Int? {
        val statement = StringBuilder(
            """
                SELECT DISTINCT COUNT(*)
                FROM circles c
                    INNER JOIN users circle_user ON c.user_id = circle_user.id
                    LEFT JOIN deployments ON c.id = deployments.circle_id
                WHERE 1 = 1
            """
        )
        name?.let { statement.appendln("AND c.name ILIKE ?") }
        statement.appendln("AND c.workspace_id = ?")

        return this.jdbcTemplate.queryForObject(
            statement.toString(),
            createParametersArray(name, true, workspaceId)
        ) { rs, _ ->
            rs.getInt(1)
        }
    }

    private fun executeCircleSimpleCountQuery(name: String?, except: String?, workspaceId: String): Int? {
        val statement = StringBuilder(
            """
                SELECT DISTINCT COUNT(*)
                FROM circles c                    
                WHERE 1 = 1
            """
        )
        name?.let { statement.appendln("AND c.name ILIKE ?") }
        except?.let { statement.appendln("AND c.id <> ?") }
        statement.appendln("AND c.workspace_id = ?")

        return this.jdbcTemplate.queryForObject(
            statement.toString(),
            createParametersSimpleCircleArray(name, except, workspaceId)
        ) { rs, _ ->
            rs.getInt(1)
        }
    }

    private fun executeCircleSimpleActiveCountQuery(name: String?, except: String?, workspaceId: String): Int? {
        val statement = StringBuilder(
            """
                SELECT DISTINCT COUNT(*)
                FROM circles c                    
                    INNER JOIN deployments ON c.id = deployments.circle_id
                WHERE 1 = 1
                    AND deployments.status NOT IN ('NOT_DEPLOYED', 'DEPLOY_FAILED')
            """
        )
        name?.let { statement.appendln("AND c.name ILIKE ?") }
        except?.let { statement.appendln("AND c.id <> ?") }
        statement.appendln("AND c.workspace_id = ?")

        return this.jdbcTemplate.queryForObject(
            statement.toString(),
            createParametersSimpleCircleArray(name, except, workspaceId)
        ) { rs, _ ->
            rs.getInt(1)
        }
    }

    private fun executeActiveCircleCountQuery(name: String?, workspaceId: String): Int? {
        val statement = StringBuilder(
            """
                    SELECT DISTINCT COUNT(*)
                    FROM circles c
                             INNER JOIN deployments d ON c.id = d.circle_id
                    WHERE 1 = 1
                    AND d.status NOT IN ('NOT_DEPLOYED', 'DEPLOY_FAILED')
                   """
        )

        name?.let { statement.appendln("AND c.name ILIKE ?") }
        statement.appendln("AND c.workspace_id = ?")

        return this.jdbcTemplate.queryForObject(
            statement.toString(),
            createParametersArray(name, true, workspaceId)
        ) { rs, _ ->
            rs.getInt(1)
        }
    }

    private fun executeInactiveCircleCountQuery(name: String?, workspaceId: String): Int? {
        val statement = StringBuilder(
            """
                SELECT DISTINCT COUNT(*)
                FROM circles c
                         LEFT JOIN deployments d ON c.id = d.circle_id
                WHERE 1 = 1
                    AND (d.circle_id IS NULL
                    OR  c.id NOT IN
                    (
                        SELECT DISTINCT d.circle_id
                        FROM deployments d
                        WHERE d.status IN ('DEPLOYING', 'DEPLOYED', 'UNDEPLOYING')
                        AND d.workspace_id = ?
                    ))
                """
        )

        name?.let { statement.appendln("AND c.name ILIKE ?") }
        statement.appendln("AND c.workspace_id = ?")

        return this.jdbcTemplate.queryForObject(
            statement.toString(),
            createParametersArray(name, false, workspaceId)
        ) { rs, _ ->
            rs.getInt(1)
        }
    }

    private fun createActiveCircleQuery(name: String?, isPercentage: Boolean? = null): StringBuilder {
        val statement = StringBuilder(
            """
                    SELECT circles.id                  AS circle_id,
                           circles.name                AS circle_name,
                           circles.reference           AS circle_reference,
                           circles.created_at          AS circle_created_at,
                           circles.matcher_type        AS circle_matcher_type,
                           circles.rules               AS circle_rules,
                           circles.imported_kv_records AS circle_imported_kv_records,
                           circles.imported_at         AS circle_imported_at,
                           circles.default_circle      AS circle_default,
                           circles.workspace_id        AS circle_workspace_id,
                           circles.percentage          AS circle_percentage,
                           circle_user.id              AS circle_user_id,
                           circle_user.name            AS circle_user_name,
                           circle_user.email           AS circle_user_email,
                           circle_user.photo_url       AS circle_user_photo_url,
                           circle_user.created_at      AS circle_user_created_at,
                           TRUE                        AS circle_active
                    FROM circles
                             LEFT JOIN users circle_user ON circles.user_id = circle_user.id
                             INNER JOIN deployments ON circles.id = deployments.circle_id
                    WHERE 1 = 1
                            AND deployments.status NOT IN ('NOT_DEPLOYED', 'DEPLOY_FAILED')
                """
        )

        if (isPercentage != null && isPercentage) statement.append("AND MATCHER_TYPE= 'PERCENTAGE'")
        name?.let { statement.appendln("AND circles.name ILIKE ?") }
        statement.appendln("AND circles.workspace_id = ?")
        statement.appendln("ORDER BY circles.name")

        return statement
    }

    private fun createInactiveCircleQuery(name: String?, isPercentage: Boolean? = null): StringBuilder {
        val statement = StringBuilder(
            """
                    SELECT circles.id                  AS circle_id,
                           circles.name                AS circle_name,
                           circles.reference           AS circle_reference,
                           circles.created_at          AS circle_created_at,
                           circles.matcher_type        AS circle_matcher_type,
                           circles.rules               AS circle_rules,
                           circles.imported_kv_records AS circle_imported_kv_records,
                           circles.imported_at         AS circle_imported_at,
                           circles.default_circle      AS circle_default,
                           circles.workspace_id        AS circle_workspace_id,
                           circles.percentage          AS circle_percentage,
                           circle_user.id              AS circle_user_id,
                           circle_user.name            AS circle_user_name,
                           circle_user.email           AS circle_user_email,
                           circle_user.photo_url       AS circle_user_photo_url,
                           circle_user.created_at      AS circle_user_created_at,
                           FALSE                       AS circle_active
                    FROM circles
                             LEFT JOIN users circle_user ON circles.user_id = circle_user.id
                             LEFT JOIN deployments ON circles.id = deployments.circle_id
                    WHERE 1 = 1
                           AND (deployments.circle_id IS NULL
                           OR  circles.id NOT IN
                           (
                               SELECT DISTINCT d.circle_id
                               FROM deployments d
                               WHERE d.status IN ('DEPLOYING', 'DEPLOYED', 'UNDEPLOYING')
                               AND d.workspace_id = ?
                           ))
                """
        )

        if (isPercentage != null && isPercentage) statement.append("AND MATCHER_TYPE= 'PERCENTAGE'")
        name?.let { statement.appendln("AND circles.name ILIKE ?") }
        statement.appendln("AND circles.workspace_id = ?")
        statement.appendln("ORDER BY circles.name")

        return statement
    }

    private fun createCircleQuery(name: String?): StringBuilder {
        val statement = StringBuilder(
            """
                    SELECT circles.id                  AS circle_id,
                           circles.name                AS circle_name,
                           circles.reference           AS circle_reference,
                           circles.created_at          AS circle_created_at,
                           circles.matcher_type        AS circle_matcher_type,
                           circles.rules               AS circle_rules,
                           circles.imported_kv_records AS circle_imported_kv_records,
                           circles.imported_at         AS circle_imported_at,
                           circles.default_circle      AS circle_default,
                           circles.workspace_id        AS circle_workspace_id,
                           circles.percentage          AS circle_percentage,
                           circle_user.id              AS circle_user_id,
                           circle_user.name            AS circle_user_name,
                           circle_user.email           AS circle_user_email,
                           circle_user.photo_url       AS circle_user_photo_url,
                           circle_user.created_at      AS circle_user_created_at,
                           CASE 
                            WHEN (deployments.status NOT IN ('NOT_DEPLOYED', 'DEPLOY_FAILED')) THEN TRUE 
                            ELSE FALSE 
                           END AS circle_active
                    FROM circles
                             INNER JOIN users circle_user ON circles.user_id = circle_user.id
                             LEFT JOIN deployments ON circles.id = deployments.circle_id
                    WHERE 1 = 1
                """
        )

        name?.let { statement.appendln("AND circles.name ILIKE ?") }
        statement.appendln("AND circles.workspace_id = ?")
        statement.appendln("ORDER BY circles.name")

        return statement
    }

    private fun createSimpleCircleActiveQuery(name: String?, except: String?): StringBuilder {
        val statement = StringBuilder(
            """
                    SELECT circles.id                  AS circle_id,
                           circles.name                AS circle_name,
                           circles.reference           AS circle_reference,
                           circles.created_at          AS circle_created_at,
                           circles.imported_kv_records AS circle_imported_kv_records,
                           circles.imported_at         AS circle_imported_at,
                           circles.default_circle      AS circle_default,
                           circles.workspace_id        AS circle_workspace_id                          
                    FROM circles                             
                            INNER JOIN deployments ON circles.id = deployments.circle_id
                    WHERE 1 = 1
                            AND deployments.status NOT IN ('NOT_DEPLOYED', 'DEPLOY_FAILED')
                """
        )

        name?.let { statement.appendln("AND circles.name ILIKE ?") }
        except?.let { statement.appendln("AND circles.id <> ?") }
        statement.appendln("AND circles.workspace_id = ?")
        statement.appendln("ORDER BY circles.name")

        return statement
    }

    private fun createSimpleCircleQuery(name: String?, except: String?): StringBuilder {
        val statement = StringBuilder(
            """
                    SELECT circles.id                  AS circle_id,
                           circles.name                AS circle_name,
                           circles.reference           AS circle_reference,
                           circles.created_at          AS circle_created_at,
                           circles.imported_kv_records AS circle_imported_kv_records,
                           circles.imported_at         AS circle_imported_at,
                           circles.default_circle      AS circle_default,
                           circles.workspace_id        AS circle_workspace_id                          
                    FROM circles                             
                    WHERE 1 = 1
                """
        )

        name?.let { statement.appendln("AND circles.name ILIKE ?") }
        except?.let { statement.appendln("AND circles.id <> ?") }
        statement.appendln("AND circles.workspace_id = ?")
        statement.appendln("ORDER BY circles.name")

        return statement
    }

    override fun countGroupedByStatus(workspaceId: String): List<CircleCount> {
        return this.countGroupedByStatus(workspaceId, null)
    }

    override fun countGroupedByStatus(workspaceId: String, name: String?): List<CircleCount> {
        val query = this.createCountCircleWithStatusByWorkspaceQuery()
        val parameters = mutableListOf(workspaceId, workspaceId)

        name?.let {
            query.append(" AND circles.name ILIKE ? ")
            parameters.add("%$name%")
        }
        query.append(" GROUP BY circle_status ")

        return this.jdbcTemplate.query(
            query.toString(),
            parameters.toTypedArray(),
            circleMetricExtractor
        )?.toList()
            ?: emptyList()
    }

    private fun createCountCircleWithStatusByWorkspaceQuery(): StringBuilder {
        return StringBuilder(
            """
                    SELECT  COUNT(circles.id)                                       AS total,
                            CASE deployments.status 
                                WHEN 'DEPLOYED' THEN 'ACTIVE'
                                ELSE 'INACTIVE'
                            END                                                     AS circle_status
                    FROM circles circles
                            LEFT JOIN deployments deployments ON circles.id = deployments.circle_id
                    WHERE circles.workspace_id = ?
                        AND (
                            deployments.id IN 
                                (
                                    SELECT DISTINCT ON (circle_id) id 
                                    FROM deployments
                                    WHERE workspace_id = ?
                                    ORDER BY circle_id, status, created_at DESC
                                )
                            OR deployments.id IS NULL
                        )
            """
        )
    }

    override fun getNotDefaultCirclesAverageLifeTime(workspaceId: String): Duration {
        val query = """
                SELECT  EXTRACT(epoch FROM DATE_TRUNC('second', AVG((NOW() - circles.created_at)))) AS average_life_time 
                FROM circles circles
                WHERE circles.workspace_id = ?
                    AND NOT circles.default_circle
        """

        return this.jdbcTemplate.queryForObject(
            query,
            arrayOf(workspaceId)
        ) { rs, _ ->
            Duration.ofSeconds(rs.getLong(1))
        } ?: Duration.ZERO
    }

    override fun findCirclesHistory(workspaceId: String, name: String?, pageRequest: PageRequest): Page<CircleHistory> {
        val totalItems = this.count(workspaceId, name)
        val parameters = mutableListOf<Any>(workspaceId, workspaceId)
        val query = createFindCirclesHistoryQuery()

        name?.let {
            query.append(" AND circles.name ILIKE ? ")
            parameters.add("%$name%")
        }

        query.appendln(" ORDER BY circle_status, circle_life_time DESC ")
        query.appendln(createPaginationAppend())
        parameters.add(pageRequest.size)
        parameters.add(pageRequest.size * pageRequest.page)

        val result = this.jdbcTemplate.query(
            query.toString(),
            parameters.toTypedArray(),
            circleHistoryExtractor
        )?.toList()
            ?: emptyList()

        return Page(result, pageRequest.page, pageRequest.size, totalItems)
    }

    private fun createPaginationAppend(): String {
        return """  
                    LIMIT ? 
                    OFFSET ? 
                """
    }

    private fun createFindCirclesHistoryQuery() = StringBuilder(
        """
                SELECT  circles.id                                                                                                  AS circle_id,
                        circles.name                                                                                                AS circle_name,
                        EXTRACT(epoch FROM DATE_TRUNC('second', (NOW() - circles.created_at)))                                      AS circle_life_time,
                        CASE deployments.status 
                            WHEN 'DEPLOYED' THEN 'ACTIVE'
                            ELSE 'INACTIVE'
                        END                                                                                                         AS circle_status,
                        GREATEST(circles.created_at, deployments.deployed_at, deployments.created_at, deployments.undeployed_at)    AS last_updated_at
                FROM circles circles
                        LEFT JOIN deployments deployments ON deployments.circle_id = circles.id
                WHERE circles.workspace_id = ?
                    AND (
                            deployments.id IN 
                                (
                                    SELECT DISTINCT ON (circle_id) id 
                                    FROM deployments
                                    WHERE workspace_id = ?
                                    ORDER BY circle_id, status, created_at DESC
                                )
                            OR deployments.id IS NULL
                        )
        """
    )

    override fun count(workspaceId: String): Int {
        return this.count(workspaceId, null)
    }

    override fun count(workspaceId: String, name: String?): Int {
        val parameters = mutableListOf(workspaceId)
        val query = StringBuilder(
            """
                    SELECT  COUNT(circles.id)   AS total
                    FROM circles circles
                    WHERE circles.workspace_id = ?
            """
        )

        name?.let {
            query.append(" AND circles.name ILIKE ? ")
            parameters.add("%$name%")
        }

        return this.jdbcTemplate.queryForObject(
            query.toString(),
            parameters.toTypedArray()
        ) { rs, _ ->
            rs.getInt(1)
        } ?: 0
    }

    override fun findByWorkspaceId(workspaceId: String): Circles {
        val statement = StringBuilder(BASE_QUERY_STATEMENT)
            .appendln("AND circles.workspace_id = ?")

        return Circles(
            this.jdbcTemplate.query(statement.toString(), arrayOf(workspaceId), circleExtractor)!!
        )
    }

    override fun countPercentageByWorkspaceId(workspaceId: String): Int {
        val parameters = mutableListOf(workspaceId, workspaceId)
        val query = StringBuilder(
            """
                    SELECT  SUM(circles.percentage)   AS total
                    FROM circles circles
                     
                    WHERE circles.workspace_id = ?
                    AND
                      circles.id IN
                      (
                               SELECT DISTINCT d.circle_id
                               FROM deployments d
                               WHERE d.status IN ('DEPLOYING', 'DEPLOYED', 'UNDEPLOYING')
                               AND d.workspace_id = ?
                           )
            """
        )
        return this.jdbcTemplate.queryForObject(
            query.toString(),
            parameters.toTypedArray()
        ) { rs, _ ->
            rs.getInt(1)
        } ?: 0
    }

    override fun findCirclesPercentage(workspaceId: String, name: String?, active: Boolean, pageRequest: PageRequest?): Page<Circle> {
        val count = executeCountQueryPercentage(name, active, workspaceId)
        val statement = when (active) {
            true -> createActiveCircleQuery(name, true)
            else -> createInactiveCircleQuery(name, true)
        }

        val result = this.jdbcTemplate.query(
            statement.toString(),
            createParametersArray(name, active, workspaceId),
            circleExtractor
        )

        val pageUpdated = pageRequest ?: PageRequest(0, result?.size ?: 0)
        return Page(result?.toList() ?: emptyList(), pageUpdated.page, pageUpdated.size, count ?: 0)
    }

    private fun executeCountQueryPercentage(name: String?, active: Boolean, workspaceId: String): Int? {
        return when (active) {
            true -> executeActiveCirclePercentageCountQuery(name, workspaceId)
            else -> executeInactiveCirclePercentageCountQuery(name, workspaceId)
        }
    }

    private fun executeActiveCirclePercentageCountQuery(name: String?, workspaceId: String): Int? {
        val statement = StringBuilder(
            """
                    SELECT COUNT(distinct c.id)
                    FROM circles c
                             INNER JOIN deployments d ON c.id = d.circle_id
                    WHERE 1 = 1
                    AND c.id NOT IN 
                    (
                        SELECT DISTINCT d.circle_id
                        FROM deployments d
                        WHERE d.status IN ('NOT_DEPLOYED', 'UNDEPLOYED')
                    )
                    AND c.matcher_type = 'PERCENTAGE'
               """
        )

        name?.let { statement.appendln("AND c.name ILIKE ?") }
        statement.appendln("AND c.workspace_id = ?")

        return this.jdbcTemplate.queryForObject(
            statement.toString(),
            createParametersArray(name, null, workspaceId)
        ) { rs, _ ->
            rs.getInt(1)
        }
    }

    private fun executeInactiveCirclePercentageCountQuery(name: String?, workspaceId: String): Int? {
        val statement = StringBuilder(
            """
                SELECT COUNT(distinct c.id)
                FROM circles c
                         LEFT JOIN deployments d ON c.id = d.circle_id
                WHERE 1 = 1
                    AND d.circle_id IS NULL
                    AND  c.id NOT IN
                    (
                        SELECT DISTINCT d.circle_id
                        FROM deployments d
                        WHERE d.status IN ('DEPLOYING', 'DEPLOYED', 'UNDEPLOYING')
                        
                    )
                    AND c.matcher_type = 'PERCENTAGE'
                    
                """
        )

        name?.let { statement.appendln("AND c.name ILIKE ?") }
        statement.appendln("AND c.workspace_id = ?")

        return this.jdbcTemplate.queryForObject(
            statement.toString(),
            createParametersArray(name, null, workspaceId)
        ) { rs, _ ->
            rs.getInt(1)
        }
    }
}
