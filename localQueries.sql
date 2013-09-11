USE steelwrapped;

SELECT * FROM account;
SELECT * FROM email;
SELECT * FROM `to`;
SELECT * FROM cc;
SELECT * FROM bcc;
SELECT * FROM smtp_references;

DROP TABLE IF EXISTS account;

SELECT e.*,
`to`.address AS to_address,
cc.address AS cc_address,
bcc.address AS bcc_address,
r.references_email_id
FROM email e
LEFT JOIN `to` ON `to`.email_id = e.id
LEFT JOIN cc ON cc.email_id = e.id
LEFT JOIN bcc ON bcc.email_id = e.id
LEFT JOIN smtp_references r ON r.email_id = e.id
WHERE e.id = 3;