export interface RouteHookParams {
  controller?: Record<string, unknown>
  parent: {
    apiVersion: 'charlescd.io/v1'
    kind: 'CharlesRoutes'
    metadata: Record<string, unknown>
    spec: {
      circles: {
        components: {
          name: string
          tag: string
        }[]
        default: boolean
        id: string
      }[]
    }
  }
  children: RouteChildren
  finalizing: boolean
}

export interface RouteChildren {
  'VirtualService.networking.istio.io/v1beta1': VirtualServiceSpec,
  'DestinationRule.networking.istio.io/v1beta1': DestinationRuleSpec
}

export interface HookParams {
  controller: Record<string, unknown>
  parent: {
    apiVersion: 'charlescd.io/v1'
    kind: 'CharlesDeployment'
    metadata: Record<string, unknown>
    spec: {
      circleId: string
      deploymentId: string
      components: { chart: string, name: string, tag: string }[]
    }
  }
  children: {
    'Deployment.apps/v1': DeploymentSpec,
    'Service.v1': ServiceSpec
  }
  finalizing: boolean
}

export interface SpecMetadata {
  creationTimestamp: string
  generation?: number // only on deployment
  name: string
  namespace: string
  ownerReferences: unknown[]
  resourceVersion: string
  uid: string
  labels: {
    app: string
    circle_id: string
    deployment_id: string
    'controller-uid': string
    version?: string // only on deployment
    service?: string // only on service
  }
}

export interface SpecStatus {
  availableReplicas: number
  conditions: {
    lastTransitionTime: string
    lastUpdateTime: string
    message: string
    reason: string
    status: 'True' | 'False' // TODO: check if this can be other values
    type: 'Progressing' | 'Available'  // TODO: check if this can be other values
  }[]
  observedGeneration: number
  readyReplicas: number
  replicas: number
  updatedReplicas: number
}

export interface DeploymentSpec {
  [key: string]: {
    apiVersion: string
    kind: string
    metadata: SpecMetadata
    status: SpecStatus
    spec: unknown
  }
}

export interface VirtualServiceSpec {
  [key: string]: {
    apiVersion: string
    kind: string
    metadata?: SpecMetadata
    spec: {
      gateways: string[]
      hosts: string[]
      http: {
        match?: {
          headers: {
            cookie?: {
              regex: string
            }
            'x-circle-id'?: {
              exact: string
            }
          }
        }[],
        route: {
          destination: {
            host: string
            subset: string
          }
          headers: {
            request: {
              set: {
                'x-circle-source': string
              }
            },
            response: {
              set: {
                'x-circle-source': string
              }
            }
          }
        }[]
      }[]
    }
  }
}

export interface DestinationRuleSpec {
  [key: string]: {
    apiVersion: string
    kind: string
    metadata?: SpecMetadata
    spec: {
      host: string
      subsets: {
        labels: {
          circleId: string
          component: string
          tag: string
        }
        name: string
      }[]
    }
  }
}

export interface ServiceSpec {
  [key: string]: {
    apiVersion: string
    kind: string
    metadata: SpecMetadata
    spec: unknown
    status: {
      loadBalancer?: Record<string, unknown>
    }
  }
}
