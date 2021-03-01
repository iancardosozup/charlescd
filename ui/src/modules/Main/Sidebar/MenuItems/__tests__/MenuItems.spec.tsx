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

import React from 'react';
import { render, fireEvent, screen } from 'unit-test/testUtils';
import routes from 'core/constants/routes';
import { genMenuId } from 'core/utils/menu';
import * as utilsAuth from 'core/utils/auth';
import MenuItems from '../index';

const originalWindow = { ...window };

beforeEach(() => {
  delete window.location;

  window.location = {
    ...window.location,
    pathname: routes.workspaces
  };
});

afterEach(() => {
  window = originalWindow;
});

test('renders sidebar menu Items', async () => {
  render(
    <MenuItems isExpanded expandMenu={() => jest.fn()} />
  );

  const links = screen.getByTestId('sidebar-links');

  const workspacesId = genMenuId(routes.workspaces);
  const accountId = genMenuId(routes.account);

  expect(screen.getByTestId(workspacesId)).toBeInTheDocument();
  expect(screen.getByTestId(accountId)).toBeInTheDocument();
  expect(links.children.length).toBe(3);
});

test('testing outside click menu Items', async () => {
  const onOutSideCick = jest.fn();
  const props = {
    isExpanded: true
  };

  render(
    <div onClick={onOutSideCick} data-testid="external-div">
      <MenuItems isExpanded={props.isExpanded} expandMenu={() => jest.fn()} />
    </div>
  );
  const externalDiv = screen.getByTestId('external-div');
  const links = screen.getByTestId('sidebar-links');

  const workspacesId = genMenuId(routes.workspaces);
  const accountId = genMenuId(routes.account);

  expect(screen.getByTestId(workspacesId)).toBeInTheDocument();
  expect(screen.getByTestId(accountId)).toBeInTheDocument();
  expect(links.children.length).toBe(3);
  fireEvent.click(externalDiv);
  expect(props.isExpanded).toBeTruthy();
});

test('testing expand menu click', async () => {
  const onClickExpand = jest.fn();

  const isExpanded = false;

  render(
    <MenuItems isExpanded={isExpanded} expandMenu={onClickExpand} />
  );
  const links = screen.getByTestId('sidebar-links');
  const workspacesId = genMenuId(routes.workspaces);
  const accountId = genMenuId(routes.account);

  expect(screen.getByTestId(workspacesId)).toBeInTheDocument();
  expect(screen.getByTestId(accountId)).toBeInTheDocument();
  expect(links.children.length).toBe(3);

  fireEvent.click(links.children[1]);

  expect(onClickExpand).toHaveBeenCalled();
});

test('should show main menu for non-root user', () => {
  const onClickExpand = jest.fn();
  const isExpanded = true;

  render(
    <MenuItems isExpanded={isExpanded} expandMenu={onClickExpand} />
  );

  expect(screen.getByText('Workspaces')).toBeInTheDocument();
  expect(screen.getByText('Account')).toBeInTheDocument();
});

test('should show root main menu for root user', () => {
  const onClickExpand = jest.fn();
  const isExpanded = true;
  jest.spyOn(utilsAuth, 'isRoot').mockReturnValue(true);

  render(
    <MenuItems isExpanded={isExpanded} expandMenu={onClickExpand} />
  );

  expect(screen.getByText('Workspaces')).toBeInTheDocument();
  expect(screen.getByText('Users')).toBeInTheDocument();
  expect(screen.getByText('User Group')).toBeInTheDocument();
  expect(screen.getByText('Account')).toBeInTheDocument();
});

// TODO resolve warning
test('should show workspace menu', () => {
  const onClickExpand = jest.fn();
  const isExpanded = true;
  delete window.location;

  window.location = {
    ...window.location,
    pathname: routes.circles
  };

  localStorage.setItem('workspace', '1234567890');

  render(
    <MenuItems isExpanded={isExpanded} expandMenu={onClickExpand} />
  );

  expect(screen.getByText('Circles')).toBeInTheDocument();
  expect(screen.getByText('Hypotheses')).toBeInTheDocument();
  expect(screen.getByText('Modules')).toBeInTheDocument();
  expect(screen.getByText('Metrics')).toBeInTheDocument();
  expect(screen.getByText('Settings')).toBeInTheDocument();
});