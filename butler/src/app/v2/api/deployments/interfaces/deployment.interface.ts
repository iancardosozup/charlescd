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

import { CdConfiguration, Component } from './'
import { MetadataScopeEnum } from '../enums/metadata-scope.enum'

export interface Deployment {
    id: string

    authorId: string

    callbackUrl: string

    createdAt: Date

    cdConfiguration: CdConfiguration

    circleId: string

    components?: DeploymentComponent[]

    defaultCircle: boolean

    metadata: Metadata | null
}

export interface Metadata {
    scope: MetadataScopeEnum
    content: Record<string, string>
}

export type DeploymentComponent = Omit<Component, 'deployment'>
