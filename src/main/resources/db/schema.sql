-- ========================
-- EMPLEADOS
-- ========================
CREATE TABLE IF NOT EXISTS empleados (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    codigo      VARCHAR(20) UNIQUE,
    nombre      VARCHAR(100),
    usuario     VARCHAR(50) UNIQUE,
    password    VARCHAR(100),
    rol         VARCHAR(20),
    correo      VARCHAR(100),
    telefono    VARCHAR(20),
    activo      BOOLEAN DEFAULT TRUE
);

-- ========================
-- CATEGORIAS
-- ========================
CREATE TABLE IF NOT EXISTS categorias (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100),
    descripcion VARCHAR(255)
);

-- ========================
-- PROVEEDORES
-- ========================
CREATE TABLE IF NOT EXISTS proveedores (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(20) UNIQUE,
    razon_social    VARCHAR(100),
    nit             VARCHAR(20) UNIQUE,
    telefono        VARCHAR(20),
    correo          VARCHAR(100),
    direccion       VARCHAR(255),
    dias_credito    INT DEFAULT 0,
    activo          BOOLEAN DEFAULT TRUE
);

-- ========================
-- PRODUCTOS
-- ========================
CREATE TABLE IF NOT EXISTS productos (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(20) UNIQUE,
    nombre          VARCHAR(100),
    descripcion     VARCHAR(255),
    precio_compra   DOUBLE,
    precio_venta    DOUBLE,
    stock_actual    INT DEFAULT 0,
    stock_minimo    INT DEFAULT 0,
    stock_maximo    INT DEFAULT 0,
    categoria_id    INT,
    proveedor_id    INT,
    activo          BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

-- ========================
-- CLIENTES
-- ========================
CREATE TABLE IF NOT EXISTS clientes (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    codigo              VARCHAR(20) UNIQUE,
    cedula              VARCHAR(20) UNIQUE,
    nombre              VARCHAR(100),
    telefono            VARCHAR(20),
    correo              VARCHAR(100),
    numero_tarjeta      VARCHAR(20) UNIQUE,
    puntos_acumulados   INT DEFAULT 0,
    activo              BOOLEAN DEFAULT TRUE
);

-- ========================
-- VENTAS
-- ========================
CREATE TABLE IF NOT EXISTS ventas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    numero_ticket   VARCHAR(20) UNIQUE,
    fecha           DATE,
    hora            TIME,
    total           DOUBLE,
    metodo_pago     VARCHAR(20),
    estado          VARCHAR(20),
    empleado_id     INT,
    cliente_id      INT,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

-- ========================
-- DETALLE VENTA
-- ========================
CREATE TABLE IF NOT EXISTS detalle_venta (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    venta_id        INT,
    producto_id     INT,
    cantidad        INT,
    precio_unitario DOUBLE,
    subtotal        DOUBLE,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- ========================
-- ORDENES DE COMPRA
-- ========================
CREATE TABLE IF NOT EXISTS ordenes_compra (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    numero_orden    VARCHAR(20) UNIQUE,
    fecha           DATE,
    fecha_entrega   DATE,
    total           DOUBLE,
    estado          VARCHAR(20),
    proveedor_id    INT,
    empleado_id     INT,
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

-- ========================
-- DETALLE ORDEN
-- ========================
CREATE TABLE IF NOT EXISTS detalle_orden (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    orden_id        INT,
    producto_id     INT,
    cantidad        INT,
    precio_unitario DOUBLE,
    subtotal        DOUBLE,
    FOREIGN KEY (orden_id) REFERENCES ordenes_compra(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- ========================
-- DEVOLUCIONES
-- ========================
CREATE TABLE IF NOT EXISTS devoluciones (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    numero_devolucion   VARCHAR(20) UNIQUE,
    fecha               DATE,
    motivo              VARCHAR(255),
    monto_reembolso     DOUBLE,
    venta_id            INT,
    empleado_id         INT,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

-- ========================
-- CIERRE DE CAJA
-- ========================
CREATE TABLE IF NOT EXISTS cierres_caja (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    numero_cierre       VARCHAR(20) UNIQUE,
    fecha               DATE,
    turno               VARCHAR(20),
    efectivo_esperado   DOUBLE,
    efectivo_contado    DOUBLE,
    diferencia          DOUBLE,
    estado              VARCHAR(20),
    empleado_id         INT,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

-- ========================
-- MOVIMIENTOS INVENTARIO
-- ========================
CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fecha       TIMESTAMP,
    tipo        VARCHAR(20),
    cantidad    INT,
    motivo      VARCHAR(255),
    producto_id INT,
    empleado_id INT,
    orden_id    INT,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    FOREIGN KEY (orden_id) REFERENCES ordenes_compra(id)
);

-- ========================
-- FIDELIZACION
-- ========================
CREATE TABLE IF NOT EXISTS puntos_fidelizacion (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fecha       TIMESTAMP,
    puntos      INT,
    tipo        VARCHAR(20),
    cliente_id  INT,
    venta_id    INT,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (venta_id) REFERENCES ventas(id)
);