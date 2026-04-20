SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE IF EXISTS CuentaXMovimiento;
DROP TABLE IF EXISTS ProveedorXProducto;
DROP TABLE IF EXISTS Detalle_devolucion;
DROP TABLE IF EXISTS Detalle_orden_compra;
DROP TABLE IF EXISTS Detalle_venta;
DROP TABLE IF EXISTS Devoluciones;
DROP TABLE IF EXISTS Orden_compra;
DROP TABLE IF EXISTS Caja;
DROP TABLE IF EXISTS Pago_venta;
DROP TABLE IF EXISTS Movimiento_puntos;
DROP TABLE IF EXISTS Movimiento_inventario;
DROP TABLE IF EXISTS Venta;
DROP TABLE IF EXISTS Cuenta_fidelizacion;
DROP TABLE IF EXISTS Usuario;
DROP TABLE IF EXISTS Producto;
DROP TABLE IF EXISTS Proveedor;
DROP TABLE IF EXISTS Tipo_pago;
DROP TABLE IF EXISTS Tipo_movimiento_puntos;
DROP TABLE IF EXISTS Tipo_movimiento;
DROP TABLE IF EXISTS Categoria;
DROP TABLE IF EXISTS Cliente;
DROP TABLE IF EXISTS Empleado;
DROP TABLE IF EXISTS Rol;

SET REFERENTIAL_INTEGRITY TRUE;

CREATE TABLE Rol (
    id_rol INT PRIMARY KEY AUTO_INCREMENT,
    nombre_rol VARCHAR(255) UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE Empleado (
    id_empleado INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255),
    apellido VARCHAR(255),
    correo VARCHAR(255),
    telefono VARCHAR(255),
    estado_activo BOOLEAN,
    fecha_registro DATE
);

CREATE TABLE Cliente (
    id_cliente INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255),
    apellido VARCHAR(255),
    correo VARCHAR(255),
    telefono VARCHAR(255),
    direccion VARCHAR(255),
    estado_activo BOOLEAN,
    fecha_registro DATE
);

CREATE TABLE Categoria (
    id_categoria INT PRIMARY KEY AUTO_INCREMENT,
    id_categoria_padre INT,
    nombre VARCHAR(255),
    descripcion VARCHAR(255),
    estado_activo BOOLEAN,
    FOREIGN KEY (id_categoria_padre) REFERENCES Categoria(id_categoria)
);

CREATE TABLE Tipo_movimiento (
    id_tipo_movimiento INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255),
    descripcion VARCHAR(255)
);

CREATE TABLE Tipo_movimiento_puntos (
    id_tipo_movimiento_puntos INT PRIMARY KEY AUTO_INCREMENT,
    descripcion VARCHAR(255)
);

CREATE TABLE Tipo_pago (
    id_tipo_pago INT PRIMARY KEY AUTO_INCREMENT,
    descripcion VARCHAR(255)
);

CREATE TABLE Proveedor (
    id_proveedor INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255),
    correo VARCHAR(255),
    telefono VARCHAR(255),
    direccion VARCHAR(255),
    estado_activo BOOLEAN,
    fecha_registro DATE
);



CREATE TABLE Producto (
    id_producto INT PRIMARY KEY AUTO_INCREMENT,
    id_categoria INT,
    nombre VARCHAR(255),
    descripcion VARCHAR(255),
    marca VARCHAR(255),
    precio_compra DECIMAL(10,2),
    precio_venta DECIMAL(10,2),
    stock_actual INT,
    stock_minimo INT,
    estado_activo BOOLEAN,
    fecha_registro DATE,
    FOREIGN KEY (id_categoria) REFERENCES Categoria(id_categoria)
);

CREATE TABLE Usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    id_rol INT,
    id_empleado INT,
    username VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    intentos_fallidos INT,
    bloqueado_hasta TIME,
    ultimo_acceso DATE,
    estado_usuario BOOLEAN,
    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol),
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado)
);

CREATE TABLE Cuenta_fidelizacion (
    id_fidelizacion INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT,
    numero_tarjeta INT,
    puntos_actuales INT,
    fecha_creacion DATE,
    estado BOOLEAN,
    FOREIGN KEY (id_cliente) REFERENCES Cliente(id_cliente)
);

CREATE TABLE Venta (
    id_venta INT PRIMARY KEY AUTO_INCREMENT,
    id_empleado INT,
    fecha_venta DATE,
    subtotal DECIMAL(10,2),
    descuento_total INT,
    impuesto_total DECIMAL(10,2),
    total_final DECIMAL(10,2),
    estado_venta BOOLEAN,
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado)
);



CREATE TABLE Movimiento_inventario (
    id_movimiento INT PRIMARY KEY AUTO_INCREMENT,
    id_empleado INT,
    id_tipo_movimiento INT,
    id_producto INT,
    cantidad INT,
    stock_anterior INT,
    stock_nuevo INT,
    motivo VARCHAR(255),
    fecha_movimiento DATE,
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado),
    FOREIGN KEY (id_tipo_movimiento) REFERENCES Tipo_movimiento(id_tipo_movimiento),
    FOREIGN KEY (id_producto) REFERENCES Producto(id_producto)
);

CREATE TABLE Movimiento_puntos (
    id_movimiento INT PRIMARY KEY AUTO_INCREMENT,
    id_tipo_movimiento_puntos INT,
    id_venta INT,
    puntos INT,
    fecha_movimiento DATE,
    FOREIGN KEY (id_tipo_movimiento_puntos) REFERENCES Tipo_movimiento_puntos(id_tipo_movimiento_puntos),
    FOREIGN KEY (id_venta) REFERENCES Venta(id_venta)
);

CREATE TABLE Pago_venta (
    id_pago_venta INT PRIMARY KEY AUTO_INCREMENT,
    id_venta INT,
    id_tipo_pago INT,
    monto DECIMAL(10,2),
    fecha_pago DATE,
    FOREIGN KEY (id_venta) REFERENCES Venta(id_venta),
    FOREIGN KEY (id_tipo_pago) REFERENCES Tipo_pago(id_tipo_pago)
);

CREATE TABLE Caja (
    id_caja INT PRIMARY KEY AUTO_INCREMENT,
    id_empleado INT,
    id_venta INT,
    fecha_apertura DATE,
    fecha_cierre DATE,
    monto_inicial DECIMAL(10,2),
    monto_final DECIMAL(10,2),
    estado BOOLEAN,
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado),
    FOREIGN KEY (id_venta) REFERENCES Venta(id_venta)
);

CREATE TABLE Orden_compra (
    id_orden_compra INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT,
    id_empleado INT,
    fecha_creacion DATE,
    fecha_entrega DATE,
    total DECIMAL(10,2),
    estado BOOLEAN,
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor),
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado)
);

CREATE TABLE Devoluciones (
    id_devoluciones INT PRIMARY KEY AUTO_INCREMENT,
    id_empleado INT,
    id_venta INT,
    fecha_devolucion DATE,
    motivo VARCHAR(255),
    total_devuelto DECIMAL(10,2),
    estado BOOLEAN,
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado),
    FOREIGN KEY (id_venta) REFERENCES Venta(id_venta)
);



CREATE TABLE Detalle_venta (
    id_detalle_venta INT PRIMARY KEY AUTO_INCREMENT,
    id_venta INT,
    id_producto INT,
    cantidad INT,
    precio_unitario DECIMAL(10,2),
    descuento INT,
    subtotal DECIMAL(10,2),
    FOREIGN KEY (id_venta) REFERENCES Venta(id_venta),
    FOREIGN KEY (id_producto) REFERENCES Producto(id_producto)
);

CREATE TABLE Detalle_orden_compra (
    id_detalle_orden INT PRIMARY KEY AUTO_INCREMENT,
    id_orden_compra INT,
    id_producto INT,
    cantidad INT,
    costo_unitario DECIMAL(10,2),
    subtotal DECIMAL(10,2),
    FOREIGN KEY (id_orden_compra) REFERENCES Orden_compra(id_orden_compra),
    FOREIGN KEY (id_producto) REFERENCES Producto(id_producto)
);

CREATE TABLE Detalle_devolucion (
    id_detalle_devolucion INT PRIMARY KEY AUTO_INCREMENT,
    id_devoluciones INT,
    id_producto INT,
    cantidad INT,
    subtotal_devuelto DECIMAL(10,2),
    motivo VARCHAR(255),
    FOREIGN KEY (id_devoluciones) REFERENCES Devoluciones(id_devoluciones),
    FOREIGN KEY (id_producto) REFERENCES Producto(id_producto)
);

CREATE TABLE ProveedorXProducto (
    id_productoXproveedor INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT,
    id_producto INT,
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor),
    FOREIGN KEY (id_producto) REFERENCES Producto(id_producto)
);

CREATE TABLE CuentaXMovimiento (
    id_cuentaXmovimiento INT PRIMARY KEY AUTO_INCREMENT,
    id_movimiento INT,
    id_cuenta_fidelizacion INT,
    FOREIGN KEY (id_movimiento) REFERENCES Movimiento_puntos(id_movimiento),
    FOREIGN KEY (id_cuenta_fidelizacion) REFERENCES Cuenta_fidelizacion(id_fidelizacion)
);



-- ROLES
INSERT INTO Rol (id_rol, nombre_rol, descripcion) VALUES
(1, 'ADMINISTRADOR', 'Control total del sistema'),
(2, 'CAJERO', 'Encargado de ventas'),
(3, 'SUPERVISOR_INVENTARIO', 'Gestiona inventario'),
(4, 'GERENTE', 'Visualiza reportes');


-- EMPLEADOS
INSERT INTO Empleado (id_empleado, nombre, apellido, correo, telefono, estado_activo, fecha_registro) VALUES
(1, 'Andres', 'Gonzales', 'Andre@mail.com', '3001111111', TRUE, CURRENT_DATE),
(2, 'Laura', 'Gomez', 'laura@mail.com', '3002222222', TRUE, CURRENT_DATE),
(3, 'Carlos', 'Ruiz', 'carlos@mail.com', '3003333333', TRUE, CURRENT_DATE);

-- =========================
-- USUARIOS (password.hashCode())
-- admin = 3198785
-- 1234 = 1509442
-- inventario / contraseña 1234
-- cajero / contraseña 1234
-- =========================
INSERT INTO Usuario (id_usuario, id_rol, id_empleado, username, password_hash, intentos_fallidos, bloqueado_hasta, ultimo_acceso, estado_usuario) VALUES
(1, 1, 1, 'admin', '3198785', 0, NULL, CURRENT_DATE, TRUE),
(2, 3, 2, 'inventario', '1509442', 0, NULL, CURRENT_DATE, TRUE),
(3, 2, 3, 'cajero', '1509442', 0, NULL, CURRENT_DATE, TRUE);


-- CATEGORIAS
INSERT INTO Categoria (id_categoria, id_categoria_padre, nombre, descripcion, estado_activo) VALUES
(1, NULL, 'Granos', 'Productos de grano', TRUE),
(2, NULL, 'Lacteos', 'Productos lacteos', TRUE),
(3, NULL, 'Bebidas', 'Bebidas varias', TRUE);


-- PRODUCTOS

INSERT INTO Producto (id_producto, id_categoria, nombre, descripcion, marca, precio_compra, precio_venta, stock_actual, stock_minimo, estado_activo, fecha_registro) VALUES
(1, 1, 'Arroz', 'Arroz blanco 1kg', 'Diana', 3000, 4500, 20, 5, TRUE, CURRENT_DATE),
(2, 1, 'Lentejas', 'Lentejas 500g', 'La Muñeca', 2000, 3200, 4, 5, TRUE, CURRENT_DATE), -- stock bajo
(3, 2, 'Leche', 'Leche entera', 'Alpina', 2500, 3800, 15, 5, TRUE, CURRENT_DATE),
(4, 3, 'Gaseosa', 'Coca Cola 1.5L', 'CocaCola', 4000, 6000, 8, 3, TRUE, CURRENT_DATE);

-- TIPOS DE MOVIMIENTO
INSERT INTO Tipo_movimiento (id_tipo_movimiento, nombre, descripcion) VALUES
(1, 'ENTRADA', 'Entrada de inventario'),
(2, 'SALIDA', 'Salida de inventario'),
(3, 'AJUSTE', 'Ajuste de inventario');


-- MOVIMIENTOS DE INVENTARIO
INSERT INTO Movimiento_inventario 
(id_movimiento, id_empleado, id_tipo_movimiento, id_producto, cantidad, stock_anterior, stock_nuevo, motivo, fecha_movimiento) VALUES

-- Entrada arroz
(1, 1, 1, 1, 10, 10, 20, 'Compra proveedor', CURRENT_DATE),

-- Salida arroz
(2, 2, 2, 1, 5, 20, 15, 'Venta', CURRENT_DATE),

-- Ajuste lentejas (stock bajo)
(3, 2, 3, 2, 1, 5, 4, 'Conteo fisico', CURRENT_DATE),

-- Entrada leche
(4, 1, 1, 3, 10, 5, 15, 'Compra proveedor', CURRENT_DATE),

-- Salida gaseosa
(5, 3, 2, 4, 2, 10, 8, 'Venta', CURRENT_DATE);