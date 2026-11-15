The Engage SDK defines cluster structures as shown in this reference:

```json
{
  "clusters": {
    "FeaturedCluster": {
      "package": "com.google.android.engage.common.datamodel.FeaturedCluster",
      "fields": {
        "entities": {
          "type": "List<Entity>",
          "requirement": "Required",
          "adder": "addEntity(Entity)",
          "getter": "getEntities()"
        },
        "displayTimeWindows": {
          "type": "List<DisplayTimeWindow>",
          "requirement": "Optional",
          "adder": "addDisplayTimeWindow(DisplayTimeWindow)",
          "getter": "getDisplayTimeWindows()"
        },
        "allDisplayTimeWindows": {
          "type": "List<List<DisplayTimeWindow>>",
          "requirement": "Optional",
          "adder": "addAllDisplayTimeWindow(DisplayTimeWindow)",
          "adderAll": "addAllDisplayTimeWindow(List<DisplayTimeWindow>)"
        }
      },
      "associatedClusterType": "ClusterType.TYPE_FEATURED"
    },
    "ContinuationCluster": {
      "package": "com.google.android.engage.common.datamodel.ContinuationCluster",
      "fields": {
        "syncAcrossDevices": {
          "type": "Boolean",
          "requirement": "Optional",
          "setter": "setSyncAcrossDevices(boolean)",
          "getter": "getSyncAcrossDevices()"
        },
        "accountProfile": {
          "type": "AccountProfile",
          "requirement": "Optional",
          "setter": "setAccountProfile(AccountProfile)",
          "getter": "getAccountProfile()"
        },
        "entities": {
          "type": "List<Entity>",
          "requirement": "Required",
          "adder": "addEntity(Entity)",
          "getter": "getEntities()"
        },
        "displayTimeWindows": {
          "type": "List<DisplayTimeWindow>",
          "requirement": "Optional",
          "adder": "addDisplayTimeWindow(DisplayTimeWindow)",
          "getter": "getDisplayTimeWindows()"
        },
        "allDisplayTimeWindows": {
          "type": "List<List<DisplayTimeWindow>>",
          "requirement": "Optional",
          "adder": "addAllDisplayTimeWindow(DisplayTimeWindow)",
          "adderAll": "addAllDisplayTimeWindow(List<DisplayTimeWindow>)"
        }
      },
      "associatedClusterType": "ClusterType.TYPE_CONTINUATION"
    },
    "RecommendationCluster": {
      "package": "com.google.android.engage.common.datamodel.RecommendationCluster",
      "fields": {
        "entities": {
          "type": "List<Entity>",
          "requirement": "Required",
          "adder": "addEntity(Entity)",
          "getter": "getEntities()"
        },
        "title": {
          "type": "String",
          "requirement": "Required",
          "setter": "setTitle(String)",
          "getter": "getTitle()"
        },
        "subtitle": {
          "type": "String",
          "requirement": "Optional",
          "setter": "setSubtitle(String)",
          "getter": "getSubtitle()"
        },
        "actionText": {
          "type": "String",
          "requirement": "Optional",
          "setter": "setActionText(String)",
          "getter": "getActionText()"
        },
        "actionUri": {
          "type": "Uri",
          "requirement": "Optional",
          "setter": "setActionUri(Uri)",
          "getter": "getActionUri()"
        },
        "displayTimeWindows": {
          "type": "List<DisplayTimeWindow>",
          "requirement": "Optional",
          "adder": "addDisplayTimeWindow(DisplayTimeWindow)",
          "getter": "getDisplayTimeWindows()"
        },
        "allDisplayTimeWindows": {
          "type": "List<List<DisplayTimeWindow>>",
          "requirement": "Optional",
          "adder": "addAllDisplayTimeWindow(DisplayTimeWindow)",
          "adderAll": "addAllDisplayTimeWindow(List<DisplayTimeWindow>)"
        },
        "recommendationClusterType": {
          "type": "@RecommendationClusterType int",
          "requirement": "Required",
          "setter": "setRecommendationClusterType(@RecommendationClusterType int)",
          "getter": "getRecommendationClusterType()"
        }
      },
      "associatedClusterType": "ClusterType.TYPE_RECOMMENDATION"
    },
    "SubscriptionCluster": {
      "package": "com.google.android.engage.common.datamodel.SubscriptionCluster",
      "fields": {
        "accountProfile": {
          "type": "AccountProfile",
          "requirement": "Optional",
          "setter": "setAccountProfile(AccountProfile)",
          "getter": "getAccountProfile()"
        },
        "subscriptionEntities": {
          "type": "List<SubscriptionEntity>",
          "requirement": "Required",
          "adder": "addSubscriptionEntity(SubscriptionEntity)",
          "getter": "getSubscriptionEntities()"
        }
      },
      "associatedClusterType": "ClusterType.TYPE_SUBSCRIPTION"
    },
    "EngagementCluster": {
      "package": "com.google.android.engage.common.datamodel.EngagementCluster",
      "associatedClusterType": "ClusterType.TYPE_ENGAGEMENT",
      "fields": {
        "signInCardEntity": {
          "type": "SignInCardEntity",
          "requirement": "Required",
          "setter": "setSignInCardEntity(SignInCardEntity)"
        },
        "userSettingsCardEntity": {
          "type": "UserSettingsCardEntity",
          "requirement": "Required",
          "setter": "setUserSettingsCardEntity(UserSettingsCardEntity)"
        }
      }
    }
  }
}
```