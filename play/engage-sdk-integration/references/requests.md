Defines the request structures for publishing various data models in
the Engage SDK.

```json
{
  "PublishRecommendationClustersRequest": {
    "package": "com.google.android.engage.service.PublishRecommendationClustersRequest",
    "fields": {
      "recommendationClusters": {
        "type": "List<RecommendationCluster>",
        "requirement": "Required",
        "adder": "addRecommendationCluster(RecommendationCluster)",
        "getter": "getRecommendationClusters()"
      },
      "accountProfile": {
        "type": "@NonNull AccountProfile",
        "requirement": "Optional",
        "setter": "setAccountProfile(@NonNull AccountProfile)",
        "getter": "getAccountProfile()",
        "description": "Required for personalization and cross-device syncing of recommendations."
      },
      "syncAcrossDevices": {
        "type": "Boolean",
        "requirement": "Optional",
        "setter": "setSyncAcrossDevices(boolean)",
        "getter": "getSyncAcrossDevices()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_RECOMMENDATION"
  },
  "PublishFeaturedClusterRequest": {
    "package": "com.google.android.engage.service.PublishFeaturedClusterRequest",
    "fields": {
      "featuredCluster": {
        "type": "FeaturedCluster",
        "requirement": "Required",
        "setter": "setFeaturedCluster(FeaturedCluster)",
        "getter": "getFeaturedCluster()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_FEATURED"
  },
  "DeleteClustersRequest": {
    "package": "com.google.android.engage.service.DeleteClustersRequest",
    "fields": {
      "clusterTypes": {
        "type": "List<@ClusterType int>",
        "requirement": "Optional",
        "adder": "addClusterType(@ClusterType int)",
        "description": "The ClusterType enum values for the clusters being deleted. Use the 'associatedClusterType' from the respective publish request or cluster definition."
      },
      "deleteReason": {
        "type": "@DeleteReason int",
        "requirement": "Optional",
        "setter": "setDeleteReason(@DeleteReason int)",
        "getter": "getDeleteReason()"
      },
      "accountProfile": {
        "requirement": "Optional",
        "setter": "setAccountProfile(AccountProfile)",
        "type": "AccountProfile",
        "getter": "getAccountProfile()"
      },
      "syncAcrossDevices": {
        "requirement": "Optional",
        "setter": "setSyncAcrossDevices(boolean)",
        "type": "Boolean",
        "getter": "getSyncAcrossDevices()"
      }
    }
  },
  "PublishContinuationClusterRequest": {
    "package": "com.google.android.engage.service.PublishContinuationClusterRequest",
    "fields": {
      "continuationCluster": {
        "type": "ContinuationCluster",
        "requirement": "Required",
        "setter": "setContinuationCluster(ContinuationCluster)",
        "getter": "getContinuationCluster()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_CONTINUATION"
  },
  "PublishStatusRequest": {
    "package": "com.google.android.engage.service.PublishStatusRequest",
    "fields": {
      "statusCode": {
        "type": "@AppEngagePublishStatusCode int",
        "requirement": "Required",
        "setter": "setStatusCode(@AppEngagePublishStatusCode int)",
        "getter": "getStatusCode()"
      }
    }
  },
  "PublishSubscriptionRequest": {
    "package": "com.google.android.engage.service.PublishSubscriptionRequest",
    "fields": {
      "subscriptionClusters": {
        "type": "List<SubscriptionCluster>",
        "requirement": "Required"
      },
      "accountProfile": {
        "type": "AccountProfile",
        "requirement": "Required",
        "setter": "setAccountProfile(AccountProfile)",
        "getter": "getAccountProfile()"
      },
      "subscription": {
        "requirement": "Required",
        "setter": "setSubscription(SubscriptionEntity)",
        "type": "SubscriptionEntity",
        "getter": "getSubscription()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_SUBSCRIPTION"
  },
  "PublishUserAccountManagementRequest": {
    "package": "com.google.android.engage.service.PublishUserAccountManagementRequest",
    "associatedClusterType": "ClusterType.TYPE_ENGAGEMENT",
    "fields": {
      "actionUri": {
        "type": "Uri",
        "requirement": "Required"
      },
      "signInCardEntity": {
        "type": "SignInCardEntity",
        "requirement": "Required",
        "setter": "setSignInCardEntity(SignInCardEntity)"
      },
      "userSettingsCardEntity": {
        "requirement": "Required",
        "setter": "setUserSettingsCardEntity(UserSettingsCardEntity)",
        "type": "UserSettingsCardEntity"
      }
    }
  },
  "PublishShoppingCartClusterRequest": {
    "package": "com.google.android.engage.shopping.service.PublishShoppingCartClusterRequest",
    "fields": {
      "shoppingCart": {
        "requirement": "Required",
        "setter": "setShoppingCart(ShoppingCart)",
        "type": "ShoppingCart",
        "getter": "getShoppingCart()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_SHOPPING_CART"
  },
  "PublishShoppingListsRequest": {
    "package": "com.google.android.engage.shopping.service.PublishShoppingListsRequest",
    "fields": {
      "shoppingLists": {
        "type": "List<ShoppingList>",
        "requirement": "Optional",
        "adder": "addShoppingList(ShoppingList)",
        "getter": "getShoppingLists()",
        "adderAll": "addShoppingLists(List<ShoppingList>)"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_SHOPPING_LIST"
  },
  "PublishShoppingOrderTrackingClusterRequest": {
    "package": "com.google.android.engage.shopping.service.PublishShoppingOrderTrackingClusterRequest",
    "fields": {
      "shoppingOrderTrackingCluster": {
        "type": "ShoppingOrderTrackingCluster",
        "requirement": "Required",
        "setter": "setShoppingOrderTrackingCluster(ShoppingOrderTrackingCluster)",
        "getter": "getShoppingOrderTrackingCluster()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_SHOPPING_ORDER_TRACKING"
  },
  "PublishShoppingReorderClusterRequest": {
    "package": "com.google.android.engage.shopping.service.PublishShoppingReorderClusterRequest",
    "fields": {
      "reorderCluster": {
        "type": "ShoppingReorderCluster",
        "requirement": "Required",
        "setter": "setReorderCluster(ShoppingReorderCluster)",
        "getter": "getReorderCluster()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_SHOPPING_REORDER"
  },
  "PublishFoodShoppingCartsRequest": {
    "package": "com.google.android.engage.food.service.PublishFoodShoppingCartsRequest",
    "fields": {
      "foodShoppingCarts": {
        "type": "List<FoodShoppingCart>",
        "requirement": "Optional",
        "adder": "addFoodShoppingCart(FoodShoppingCart)",
        "getter": "getFoodShoppingCarts()",
        "adderAll": "addFoodShoppingCarts(List<FoodShoppingCart>)"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_FOOD_SHOPPING_CART"
  },
  "PublishFoodShoppingListsRequest": {
    "package": "com.google.android.engage.food.service.PublishFoodShoppingListsRequest",
    "fields": {
      "foodShoppingLists": {
        "type": "List<FoodShoppingList>",
        "requirement": "Optional",
        "adder": "addFoodShoppingList(FoodShoppingList)",
        "getter": "getFoodShoppingLists()",
        "adderAll": "addFoodShoppingLists(List<FoodShoppingList>)"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_FOOD_SHOPPING_LIST"
  },
  "PublishReorderClusterRequest": {
    "package": "com.google.android.engage.food.service.PublishReorderClusterRequest",
    "fields": {
      "reorderCluster": {
        "requirement": "Required",
        "setter": "setReorderCluster(FoodReorderCluster)",
        "type": "FoodReorderCluster",
        "getter": "getReorderCluster()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_FOOD_REORDER"
  },
  "PublishContinueSearchClusterRequest": {
    "package": "com.google.android.engage.travel.service.PublishContinueSearchClusterRequest",
    "fields": {
      "continueSearchCluster": {
        "type": "ContinueSearchCluster",
        "requirement": "Required",
        "setter": "setContinueSearchCluster(ContinueSearchCluster)",
        "getter": "getContinueSearchCluster()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_CONTINUE_SEARCH"
  },
  "PublishReservationClusterRequest": {
    "package": "com.google.android.engage.travel.service.PublishReservationClusterRequest",
    "fields": {
      "reservationCluster": {
        "type": "ReservationCluster",
        "requirement": "Required",
        "setter": "setReservationCluster(ReservationCluster)",
        "getter": "getReservationCluster()"
      }
    },
    "associatedClusterType": "ClusterType.TYPE_RESERVATION"
  },
  "ServiceAvailabilityRequest": {
    "package": "com.google.android.engage.service.ServiceAvailabilityRequest",
    "fields": {
      "intendedClusterTypes": {
        "type": "List<Integer>",
        "requirement": "Required",
        "adder": "addIntendedClusterType(@ClusterType int)",
        "adderAll": "addAllIntendedClusterTypes(List<Integer>)",
        "getter": "getIntendedClusterTypes()",
        "description": "The ClusterType enum values for the clusters being published (e.g. ClusterType.TYPE_ENGAGEMENT). Use the 'associatedClusterType' from the respective publish request or cluster definition."
      }
    }
  }
}
```