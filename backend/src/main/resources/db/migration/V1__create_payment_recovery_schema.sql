create table merchants (
    id uuid primary key,
    name varchar(160) not null,
    external_reference varchar(100) not null unique,
    auto_recovery_enabled boolean not null default true,
    max_automatic_recovery_amount numeric(19, 2) not null,
    created_at timestamptz not null default current_timestamp
);

create table customers (
    id uuid primary key,
    merchant_id uuid not null references merchants(id),
    external_reference varchar(100) not null,
    email varchar(320),
    successful_payment_count integer not null default 0 check (successful_payment_count >= 0),
    failed_payment_count integer not null default 0 check (failed_payment_count >= 0),
    created_at timestamptz not null default current_timestamp,
    unique (merchant_id, external_reference)
);

create table payments (
    id uuid primary key,
    merchant_id uuid not null references merchants(id),
    customer_id uuid references customers(id),
    provider_payment_id varchar(120) not null unique,
    amount numeric(19, 2) not null check (amount > 0),
    currency varchar(3) not null,
    status varchar(40) not null,
    failure_reason varchar(80),
    failed_at timestamptz,
    recovery_expires_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp
);

create table payment_attempts (
    id uuid primary key,
    payment_id uuid not null references payments(id),
    attempt_number integer not null check (attempt_number > 0),
    trigger_type varchar(40) not null,
    status varchar(40) not null,
    provider_attempt_id varchar(120),
    failure_reason varchar(80),
    attempted_at timestamptz not null default current_timestamp,
    created_at timestamptz not null default current_timestamp,
    unique (payment_id, attempt_number)
);

create table recovery_policies (
    id uuid primary key,
    merchant_id uuid references merchants(id),
    policy_code varchar(100) not null,
    version varchar(40) not null,
    name varchar(160) not null,
    content text not null,
    priority integer not null default 100,
    active boolean not null default true,
    created_at timestamptz not null default current_timestamp,
    unique (merchant_id, policy_code, version)
);

create table recovery_cases (
    id uuid primary key,
    payment_id uuid not null unique references payments(id),
    merchant_id uuid not null references merchants(id),
    correlation_id varchar(100) not null unique,
    status varchar(40) not null,
    retry_count integer not null default 0 check (retry_count >= 0),
    opened_at timestamptz not null default current_timestamp,
    closed_at timestamptz,
    created_at timestamptz not null default current_timestamp
);

create table recovery_decisions (
    id uuid primary key,
    recovery_case_id uuid not null references recovery_cases(id),
    recovery_policy_id uuid references recovery_policies(id),
    recommended_action varchar(40) not null,
    final_action varchar(40),
    confidence numeric(5, 4) check (confidence >= 0 and confidence <= 1),
    explanation text not null,
    safety_check_summary text,
    outcome varchar(40),
    created_at timestamptz not null default current_timestamp
);

create table notifications (
    id uuid primary key,
    recovery_case_id uuid not null references recovery_cases(id),
    channel varchar(40) not null,
    recipient varchar(320) not null,
    template_key varchar(100),
    body text,
    status varchar(40) not null,
    sent_at timestamptz,
    created_at timestamptz not null default current_timestamp
);

create table audit_events (
    id uuid primary key,
    correlation_id varchar(100) not null,
    aggregate_type varchar(80) not null,
    aggregate_id uuid not null,
    event_type varchar(100) not null,
    actor varchar(80) not null,
    payload text,
    occurred_at timestamptz not null default current_timestamp,
    created_at timestamptz not null default current_timestamp
);

create table experiment_runs (
    id uuid primary key,
    merchant_id uuid references merchants(id),
    name varchar(160) not null,
    strategy_version varchar(80) not null,
    dataset_size integer not null check (dataset_size > 0),
    random_seed bigint not null,
    status varchar(40) not null,
    metrics_summary text,
    started_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz not null default current_timestamp
);

create index idx_customers_merchant_id on customers(merchant_id);
create index idx_payments_merchant_status on payments(merchant_id, status);
create index idx_payment_attempts_payment_id on payment_attempts(payment_id);
create index idx_recovery_cases_status on recovery_cases(status);
create index idx_recovery_decisions_case_id on recovery_decisions(recovery_case_id);
create index idx_notifications_case_id on notifications(recovery_case_id);
create index idx_audit_events_correlation_id on audit_events(correlation_id);
create index idx_experiment_runs_merchant_id on experiment_runs(merchant_id);
