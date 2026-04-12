UPDATE sys_user
SET password_hash = '$2a$10$dgo85fF5uEjzlO8USrYMzec7DQ2woxBy6qLXX2U5w6MUq6WKS06EK',
    pwd_updated_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin'
  AND deleted_flag = 0;
