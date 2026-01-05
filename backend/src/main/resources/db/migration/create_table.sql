-- USE mings_project_db;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE,
    user_pw VARCHAR(255), -- social user don't have passwords
    user_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    
    -- about social login
    login_type ENUM('local', 'google', 'kakao') DEFAULT 'local',
    social_id VARCHAR(255) UNIQUE, 
    
    -- infomation
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- role type
    role ENUM('ROLE_USER', 'ROLE_ADMIN') DEFAULT 'ROLE_USER'
);