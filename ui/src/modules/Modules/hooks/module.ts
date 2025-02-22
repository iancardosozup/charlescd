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

import { useState, useCallback, useEffect } from 'react';
import { useDispatch } from 'core/state/hooks';
import { useHistory } from 'react-router-dom';
import { useFetch } from 'core/providers/base/hooks';
import {
  findAll,
  findById,
  create,
  deleteModule,
  update
} from 'core/providers/modules';
import routes from 'core/constants/routes';
import { delParam, updateUntitledParam, updateParam } from 'core/utils/path';
import { toogleNotification } from 'core/components/Notification/state/actions';
import { ModulePagination } from 'modules/Modules/interfaces/ModulePagination';
import { Module } from 'modules/Modules/interfaces/Module';
import {
  loadModulesAction,
  resetModulesAction
} from 'modules/Modules/state/actions';

export const useFindAllModules = (): {
  getAllModules: Function;
  response: ModulePagination;
  loading: boolean;
} => {
  const dispatch = useDispatch();
  const [modulesData, getModules] = useFetch<ModulePagination>(findAll);
  const { response, loading } = modulesData;

  const getAllModules = useCallback(
    (name: string, page = 0) => {
      getModules({ name, page });
    },
    [getModules]
  );

  useEffect(() => {
    if (response) {
      dispatch(loadModulesAction(response));
    }
  }, [response, dispatch]);

  return {
    getAllModules,
    response,
    loading
  };
};

export const useFindModule = (): {
  getModuleById: Function;
  response: Module;
  error: Response;
  loading: boolean;
} => {
  const [modulesData, getModuleById] = useFetch<Module>(findById);
  const { response, loading, error } = modulesData;
  const dispatch = useDispatch();

  useEffect(() => {
    (async () => {
      if (error) {
        const e = await error?.json?.();
        dispatch(
          toogleNotification({
            text: `${error?.status}: ${e?.message}`,
            status: 'error'
          })
        );
      }
    })();
  }, [dispatch, error]);

  return {
    getModuleById,
    response,
    error,
    loading
  };
};

export const useSaveModule = (): {
  loading: boolean;
  saveModule: Function;
} => {
  const [data, saveModule] = useFetch<Module>(create);
  const { getAllModules, response: modules } = useFindAllModules();
  const { response, error, loading } = data;
  const dispatch = useDispatch();

  useEffect(() => {
    if (response) {
      updateUntitledParam('module', response.id);
      getAllModules();
    }
  }, [response, getAllModules, dispatch]);

  useEffect(() => {
    if (modules) {
      dispatch(resetModulesAction());
      dispatch(loadModulesAction(modules));
    }
  }, [modules, dispatch]);

  useEffect(() => {
    (async () => {
      if (error) {
        const e = await error.json();
        dispatch(
          toogleNotification({
            text: `${error.status}: ${e?.message}`,
            status: 'error'
          })
        );
      }
    })();
  }, [dispatch, error]);

  return { loading, saveModule };
};

export const useDeleteModule = (
  module: Module
): {
  removeModule: Function;
  response: Module;
  error: Response;
  loading: boolean;
} => {
  const [data, removeModule] = useFetch<Module>(deleteModule);
  const { getAllModules, response: modules } = useFindAllModules();
  const { response, error, loading } = data;
  const dispatch = useDispatch();
  const history = useHistory();

  useEffect(() => {
    if (response) {
      getAllModules();
    }
  }, [response, getAllModules, history, module]);

  useEffect(() => {
    if (modules) {
      delParam('module', routes.modulesComparation, history, module?.id);
      dispatch(resetModulesAction());
      dispatch(loadModulesAction(modules));
    }
  }, [dispatch, modules, history, module]);

  useEffect(() => {
    (async () => {
      if (error) {
        const e = await error?.json?.();
        dispatch(
          toogleNotification({
            text: `${error.status}: ${e?.message}`,
            status: 'error'
          })
        );
      } else if(response) {
        dispatch(
          toogleNotification({
            text: `The module ${module?.name} has been deleted`,
            status: 'success'
          })
        );
      }
    })();
  }, [dispatch, error, response, module]);

  return {
    removeModule,
    response,
    error,
    loading
  };
};

export const useUpdateModule = (): {
  updateModule: Function;
  status: string;
} => {
  const [, , updateModulePromise] = useFetch<Module>(update);
  const { response: modules, getAllModules } = useFindAllModules();
  const [status, setStatus] = useState('');
  const [moduleId, setModuleId] = useState<string>(null);
  const history = useHistory();
  const dispatch = useDispatch();

  useEffect(() => {
    if (modules) {
      updateParam(
        'module',
        routes.modulesComparation,
        history,
        moduleId,
        `${moduleId}~view`
      );
    }
  }, [moduleId, history, modules]);

  const updateModule = useCallback(
    async (id: string, module: Module) => {
      setStatus('pending');
      setModuleId(id);
      try {
        await updateModulePromise(id, module);
        dispatch(resetModulesAction());
        getAllModules();
        setStatus('resolved');
      } catch (error) {
        setStatus('rejected');
        dispatch(
          toogleNotification({
            text: `${error.status}: ${error.statusText}`,
            status: 'error'
          })
        );
      }
    },
    [updateModulePromise, dispatch, getAllModules]
  );

  return {
    updateModule,
    status
  };
};
