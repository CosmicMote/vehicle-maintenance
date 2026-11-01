/** 'overdue' = overdue items only; 'due' = overdue items plus items due soon. */
export type ReminderThreshold = 'overdue' | 'due';
export type SmtpSecurity = 'starttls' | 'ssl' | 'none';

export interface EmailSettingsUpdate {
  enabled: boolean;
  smtp_host: string;
  smtp_port: number;
  smtp_security: SmtpSecurity;
  smtp_username: string;
  from_address: string;
  to_addresses: string;
  threshold: ReminderThreshold;
  frequency_days: number;
}

export interface EmailSettings extends EmailSettingsUpdate {
  password_configured: boolean;
  last_sent_at: string | null;
}

export interface SendReminderResult {
  sent: boolean;
  item_count: number;
  recipients: string[];
  message: string;
}
