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
import Text from 'core/components/Text';

type DotProps = {
  color: string;
}

const Item = styled.div`
  display: flex;
  margin-right: 15px;
`;

const Dot = styled.div<DotProps>`
  height: 16px;
  width: 16px;
  background-color: ${({ theme, color }) => theme.summary.colors[color]};
  border-radius: 50%;
  display: inline-block;
  margin-right: 5px;
`;

const Name = styled(Text)`
  line-height: 15px;
`;

export default {
  Item,
  Dot,
  Name
};
