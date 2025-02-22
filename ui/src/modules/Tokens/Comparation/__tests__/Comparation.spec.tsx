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

import { render, screen } from 'unit-test/testUtils';
import { FetchMock } from 'jest-fetch-mock/types';
import Comparation  from '..';

const originalWindow = { ...window };

beforeEach(() => {
  delete window.location;

  window.location = {
    ...window.location,
    pathname: '/tokens/compare',
    search: '?token=3f126d1b-c776-4c26-831d-b9ca148be910' 
  };
});

afterEach(() => {
  // eslint-disable-next-line no-native-reassign
  window = originalWindow;
});

test('render Tokens comparation', async () => {
  (fetch as FetchMock).mockResponseOnce(JSON.stringify({ name: 'token' }));
  render(<Comparation />);

  const tabpanel = await screen.findByTestId('tabpanel-token');
  expect(tabpanel).toBeInTheDocument();
});
