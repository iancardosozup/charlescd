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

import { DeploymentEntityV2 as DeploymentEntity } from '../entity/deployment.entity'
import { ConflictException, Injectable, PipeTransform } from '@nestjs/common'
import { InjectRepository } from '@nestjs/typeorm'
import { CreateDeploymentRequestDto } from '../dto/create-deployment-request.dto'
import { Repository } from 'typeorm'

@Injectable()
export class DefaultCircleNamespaceUniquenessPipe implements PipeTransform {

  constructor(
    @InjectRepository(DeploymentEntity)
    private readonly deploymentsRepository: Repository<DeploymentEntity>
  ) {}

  public async transform(deploymentRequest: CreateDeploymentRequestDto): Promise<CreateDeploymentRequestDto> {
    if (!deploymentRequest.circle.default) {
      return deploymentRequest
    }

    const deployment: DeploymentEntity | undefined = await this.deploymentsRepository.findOne(
      { defaultCircle: true, current: true, circleId: deploymentRequest.circle.id }
    )

    if (deployment && deployment.namespace !== deploymentRequest.namespace) {
      throw new ConflictException({
        errors: [
          {
            title: 'Invalid namespace',
            detail: 'Circle already has an active default deployment in a different namespace.',
            meta: {
              component: 'butler',
              timestamp: expect.anything()
            },
            source: {
              pointer: 'deploymentId'
            },
            status: 400
          }
        ]
      }
      )
    }

    return deploymentRequest
  }
}
