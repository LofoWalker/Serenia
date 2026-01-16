export interface UserStats {
  totalUsers: number;
  activatedUsers: number;
  freeUsers: number;
  plusUsers: number;
  maxUsers: number;
  newUsersLast7Days: number;
  newUsersLast30Days: number;
}

export interface MessageStats {
  totalUserMessages: number;
  messagesToday: number;
  messagesLast7Days: number;
  messagesLast30Days: number;
}

export interface EngagementStats {
  activeUsers: number;
  activationRate: number;
  avgMessagesPerUser: number;
}

export interface SubscriptionStats {
  totalTokensConsumed: number;
  estimatedRevenueCents: number;
  currency: string;
}

export interface Dashboard {
  users: UserStats;
  messages: MessageStats;
  engagement: EngagementStats;
  subscriptions: SubscriptionStats;
}

export interface TimelineDataPoint {
  date: string;
  value: number;
}

export interface Timeline {
  metric: string;
  data: TimelineDataPoint[];
}

export interface UserDetail {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  planType: string;
  activated: boolean;
  createdAt: string;
  messageCount: number;
  tokensUsedThisMonth: number;
  messagesSentToday: number;
}

export interface UserList {
  users: UserDetail[];
  totalCount: number;
  page: number;
  size: number;
}
