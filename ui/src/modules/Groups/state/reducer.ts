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

import map from 'lodash/map';
import { UserGroupsActionTypes, ACTION_TYPES } from './actions';
import { UserGroupPagination } from '../interfaces/UserGroupsPagination';
import { UserGroupState } from '../interfaces/UserGroupState';

export const initialListState: UserGroupPagination = {
  content: [],
  page: 0,
  size: 0,
  totalPages: 0,
  last: false
};

export const userGroupInitialState: UserGroupState = {
  list: initialListState,
  item: null
};

export const userGroupReducer = (
  state = userGroupInitialState,
  action: UserGroupsActionTypes
): UserGroupState => {
  switch (action.type) {
    case ACTION_TYPES.loadedUserGroups: {
      return {
        ...state,
        list: {
          ...action.payload,
          content: [...state.list.content, ...(action?.payload?.content ?? [])]
        }
      };
    }
    case ACTION_TYPES.loadedUserGroup: {
      return {
        ...state,
        item: action.payload
      };
    }
    case ACTION_TYPES.updateUserGroup: {
      const userGroups = map(state.list.content, userGroupItem => {
        if (userGroupItem.id === action.payload.id) {
          return action.payload;
        }

        return userGroupItem;
      });

      return {
        ...state,
        list: {
          ...state.list,
          content: [
            ...userGroups
          ]
        }
      };
    }
    case ACTION_TYPES.resetUserGroups: {
      return {
        ...state,
        list: userGroupInitialState.list
      };
    }
    default: {
      return state;
    }
  }
};
