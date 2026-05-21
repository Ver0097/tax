CREATE OR REPLACE VIEW v_yukou_all AS
SELECT 
    id,
    tax_amount,
    tax_area,
    payee,
    id_card,
    phone,
    merchant,
    channel,
    sale,
    customer_service,
    order_source,
    create_time,
    update_time
FROM yukou_info

UNION ALL

SELECT 
    id,
    tax_amount,
    tax_area,
    payee,
    id_card,
    phone,
    merchant,
    channel,
    sale,
    customer_service,
    order_source,
    create_time,
    update_time
FROM yukou_jl_info

UNION ALL

SELECT 
    id,
    tax_amount,
    tax_area,
    payee,
    id_card,
    phone,
    merchant,
    channel,
    sale,
    customer_service,
    order_source,
    create_time,
    update_time
FROM yukou_qkg_info;