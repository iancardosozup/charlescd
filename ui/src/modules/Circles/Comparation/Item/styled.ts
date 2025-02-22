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

import styled from 'styled-components';
import { slideInLeft, fadeIn } from 'core/assets/style/animate';
import InputTitleComponent from 'core/components/Form/InputTitle';
import Dropdown from 'core/components/Dropdown';
import Text from 'core/components/Text';

interface NoDataThresholds {
  colorSVG: string;
  hasTreshold: boolean;
}

const Wrapper = styled.div`
  animation: 0.2s ${slideInLeft} linear;
`;

const Actions = styled.div`
  margin-left: auto;
  display: flex;
  flex-direction: row;
  align-items: center;

  > :first-child {
    margin-left: 0px;
  }

  > :last-child {
    margin-left: 24px;
  }

  > :nth-last-child(2) {
    margin-left: 24px;
  }
`;

const Action = styled(Dropdown.Item)``;

const Release = styled.div`
  position: relative;
  height: 61px;
  z-index: ${({ theme }) => theme.zIndex.OVER_2};

  > {
    position: absolute;
  }
`;

const Layer = styled.div`
  margin-top: 40px;

  :last-child {
    padding-bottom: 85px;
  }
`;

const Content = styled.div`
  animation: 0.5s ${fadeIn} linear;
  margin-top: 15px;
  margin-left: 45px;
`;

const Link = styled.a`
  text-decoration: none;
`;

const InputTitle = styled(InputTitleComponent)`
  .input-title {
    width: 334px;
    height: 31px;
    margin-top: 1px;
  }
`;

const MetricsGroupsHeader = styled.div`
  display: flex;
  padding: 15px 0 0 30px;
  justify-content: space-between;

  span {
    padding-right: 90px;
  }
`;

const MetricsGroupsContent = styled.div`
  background-color: ${({ theme }) =>
    theme.circleGroupMetrics.content.background};
  border-radius: 5px;
  margin-top: 20px;
  margin-bottom: 10px;
  width: 550px;
`;

const MetricsGroupsCountContent = styled(Text)`
  margin: auto 60px auto 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 100px;
`;

const MetricsGroupsThresholdsContent = styled.div<NoDataThresholds>`
  margin: auto 0;
  display: flex;

  span {
    margin-left: ${({ hasTreshold }) => (hasTreshold ? '0' : '5px')};
    margin-top: ${({ hasTreshold }) => (hasTreshold ? '0' : '2px')};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80px;
  }

  svg {
    color: ${({ theme, colorSVG, hasTreshold }) =>
      hasTreshold
        ? 'transparent'
        : theme.circleGroupMetrics.execution.status[colorSVG]};
  }
`;

const MetricsGroupsNameContent = styled(Text)`
  margin: auto 20px auto 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 180px;
`;

const MetricsGroupsFooter = styled.div`
  display: flex;
  justify-content: flex-end;
  padding-bottom: 15px;
  padding-right: 15px;

  svg {
    padding-top: 4px;
    padding-left: 10px;
  }
`;

const MetricsGroupsCard = styled.div`
  display: flex;
  background-color: ${({ theme }) => theme.circleGroupMetrics.content.card};
  margin: 10px 5px 10px 15px;
  border-radius: 5px;
  width: 520px;
  height: 40px;
`;

const WarningPercentageContainer = styled.div`
  display: flex;
  align-items: center;
  flex-direction: row;
  margin-top: 15px;
  margin-bottom: 15px;

  > span {
    margin-left: 10px;
  }
`;

const FieldErrorWrapper = styled.div`
  display: flex;
  margin-top: 2px;

  span {
    margin-left: 5px;
    margin-top: 2px;
  }
`;

const A = styled.a`
  text-decoration: none;
  display: inline-flex;
  align-items: center;
`;

export default {
  A,
  Link,
  Actions,
  Action,
  Content,
  Layer,
  Release,
  Wrapper,
  InputTitle,
  MetricsGroupsContent,
  MetricsGroupsHeader,
  MetricsGroupsFooter,
  MetricsGroupsCard,
  MetricsGroupsNameContent,
  MetricsGroupsCountContent,
  MetricsGroupsThresholdsContent,
  WarningPercentageContainer,
  FieldErrorWrapper,
};
