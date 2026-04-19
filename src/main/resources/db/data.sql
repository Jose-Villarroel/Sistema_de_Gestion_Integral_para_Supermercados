-- ========================
-- EMPLEADOS POR DEFECTO
-- ========================
INSERT INTO empleados (codigo, nombre, usuario, password, rol, activo)
SELECT 'EMP001', 'Administrador', 'admin', 'admin', 'ADMIN', TRUE
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE usuario = 'admin');

INSERT INTO empleados (codigo, nombre, usuario, password, rol, activo)
SELECT 'EMP002', 'Juan Cajero', 'cajero', 'cajero', 'CAJERO', TRUE
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE usuario = 'cajero');

INSERT INTO empleados (codigo, nombre, usuario, password, rol, activo)
SELECT 'EMP003', 'Maria Supervisora', 'supervisor', 'supervisor', 'SUPERVISOR', TRUE
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE usuario = 'supervisor');

INSERT INTO empleados (codigo, nombre, usuario, password, rol, activo)
SELECT 'EMP004', 'Carlos Gerente', 'gerente', 'gerente', 'GERENTE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE usuario = 'gerente');

-- ========================
-- CATEGORIAS
-- ========================
INSERT INTO categorias (nombre, descripcion)
SELECT 'Lácteos', 'Productos lácteos y derivados'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Lácteos');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Bebidas', 'Bebidas y refrescos'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Bebidas');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Snacks', 'Pasabocas y snacks'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Snacks');

-- ========================
-- PROVEEDORES
-- ========================
INSERT INTO proveedores (codigo, razon_social, nit, telefono, correo, activo)
SELECT 'PROV001', 'Lácteos del Norte S.A.', '900111222-1',
       '3001234567', 'ventas@lacteosnorte.com', TRUE
WHERE NOT EXISTS (SELECT 1 FROM proveedores WHERE nit = '900111222-1');

-- ========================
-- PRODUCTOS
-- ========================
INSERT INTO productos (codigo, nombre, precio_compra, precio_venta,
                       stock_actual, stock_minimo, stock_maximo,
                       categoria_id, proveedor_id, activo)
SELECT 'PROD001', 'Leche Entera 1L', 2000, 3200, 50, 10, 100, 1, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'PROD001');

INSERT INTO productos (codigo, nombre, precio_compra, precio_venta,
                       stock_actual, stock_minimo, stock_maximo,
                       categoria_id, proveedor_id, activo)
SELECT 'PROD002', 'Agua Mineral 500ml', 500, 1500, 5, 10, 80, 2, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'PROD002');
