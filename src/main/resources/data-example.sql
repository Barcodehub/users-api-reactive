-- Datos de ejemplo para el microservicio de Users
-- Ejecutar después de que la tabla users sea creada por schema.sql

-- Insertar usuarios administradores
INSERT INTO users (name, email, is_admin) VALUES
('John Admin', 'john.admin@bootcamp.com', true),
('Maria Admin', 'maria.admin@bootcamp.com', true);

-- Insertar usuarios personas (participantes)
INSERT INTO users (name, email, is_admin) VALUES
('Carlos Pérez', 'carlos.perez@email.com', false),
('Ana García', 'ana.garcia@email.com', false),
('Luis Martínez', 'luis.martinez@email.com', false),
('Sofia López', 'sofia.lopez@email.com', false),
('Miguel Torres', 'miguel.torres@email.com', false),
('Laura Ramírez', 'laura.ramirez@email.com', false),
('Jorge Sánchez', 'jorge.sanchez@email.com', false),
('Patricia Díaz', 'patricia.diaz@email.com', false),
('Roberto Cruz', 'roberto.cruz@email.com', false),
('Elena Flores', 'elena.flores@email.com', false);

-- Verificar datos insertados
-- SELECT * FROM users ORDER BY is_admin DESC, name;
