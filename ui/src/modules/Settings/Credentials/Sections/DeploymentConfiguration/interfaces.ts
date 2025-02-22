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

export interface Props {
  onSave: Function;
}

export type GitProviders = 'GITHUB' | 'GITLAB';

export interface CDConfiguration {
  name: string;
  type: string;
  configurationData: {
    namespace: string;
    url: string;
    gitAccount: string;
    account: string;
    gitProvider: string;
    gitToken: string;
    provider: string;
    clientCertificate?: string;
    clientKey?: string;
    caData?: string;
    awsSID?: string;
    awsSecret?: string;
    awsRegion?: string;
    awsClusterName?: string;
  };
}

export interface DeploymentConfiguration {
  id?: string;
  name: string;
  butlerUrl: string;
  namespace: string;
  gitToken: string;
  gitProvider: GitProviders;
}

export interface Response {
  id: string;
  deploymentConfiguration?: DeploymentConfiguration;
}
