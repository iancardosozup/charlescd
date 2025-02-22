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
import ButtonRounded from 'core/components/Button/ButtonRounded';
import Avatar from 'core/components/Avatar';

const LayerTitle = styled.div`
  margin-top: 50px;
`;

const LayerUsers = styled.div`
  margin-top: 40px;
`;

const ButtonAdd = styled(ButtonRounded)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const UserList = styled.div`
  display: flex;
  flex-direction: row;
`;

const UserAvatar = styled.img`
  width: 40px;
  height: 40px;
  border-radius: 50%;
`;

const UserAvatarNoPhoto = styled(Avatar)`
  margin: 5px;
`;

const UsersCounter = styled.div`
  cursor: pointer;
  margin: 5px;
  width: 100%;
  height: 100%;
  background: ${({ theme }) => theme.avatar.counter};
  color: ${({ theme }) => theme.avatar.number};
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const FieldErrorWrapper = styled.div`
  display: flex;
  margin-top: 5px;

  span {
    margin-top: 2px;
    margin-left: 5px;
  }
`;

export default {
  FieldErrorWrapper,
  UserList,
  UserAvatar,
  ButtonAdd,
  UserAvatarNoPhoto,
  UsersCounter,
  Layer: {
    Title: LayerTitle,
    Users: LayerUsers
  }
};
