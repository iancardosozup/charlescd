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
import ComponentContentIcon from 'core/components/ContentIcon';
import ComponentLayer from 'core/components/Layer';
import { slideInLeft } from 'core/assets/style/animate';

const Wrapper = styled.div`
  animation: 0.2s ${slideInLeft} linear;
`;

const Layer = styled(ComponentLayer)`
  span + span {
    margin-top: 10px;
  }
`;

const ContentIcon = styled(ComponentContentIcon)`
  align-items: center;
`;

const Actions = styled.div`
  margin-left: auto;
  display: flex;
  flex-direction: row;

  > :last-child {
    margin-left: 36px;
  }
`;

const Groups = styled.div`
  display: flex;
  flex-direction: column;

  > div {
    margin-top: 10px;
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

export default {
  FieldErrorWrapper,
  Wrapper,
  ContentIcon,
  Layer,
  Groups,
  Actions
};
