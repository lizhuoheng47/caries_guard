import { defineStore } from 'pinia';

export type NotificationType = 'success' | 'error' | 'info' | 'warning';

export interface NotificationItem {
  id: number;
  type: NotificationType;
  title: string;
  message?: string;
  duration?: number;
}

let notificationSeed = 1;

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    items: [] as NotificationItem[],
  }),

  actions: {
    show(payload: Omit<NotificationItem, 'id'>) {
      const id = notificationSeed++;
      const item: NotificationItem = {
        id,
        duration: 4000,
        ...payload,
      };

      this.items.push(item);

      if ((item.duration ?? 0) > 0) {
        window.setTimeout(() => {
          this.remove(id);
        }, item.duration);
      }

      return id;
    },

    success(title: string, message?: string, duration = 2800) {
      return this.show({ type: 'success', title, message, duration });
    },

    error(title: string, message?: string, duration = 5200) {
      return this.show({ type: 'error', title, message, duration });
    },

    warning(title: string, message?: string, duration = 4200) {
      return this.show({ type: 'warning', title, message, duration });
    },

    info(title: string, message?: string, duration = 3200) {
      return this.show({ type: 'info', title, message, duration });
    },

    remove(id: number) {
      this.items = this.items.filter((item) => item.id !== id);
    },
  },
});
