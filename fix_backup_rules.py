with open('app/src/main/res/xml/backup_rules.xml', 'r') as f:
    content = f.read()

content = content.replace('</full-backup-content>', '    <exclude domain="database" path="inbusiness.db" />\n    <exclude domain="database" path="inbusiness.db-wal" />\n    <exclude domain="database" path="inbusiness.db-shm" />\n</full-backup-content>')

with open('app/src/main/res/xml/backup_rules.xml', 'w') as f:
    f.write(content)

with open('app/src/main/res/xml/data_extraction_rules.xml', 'r') as f:
    content = f.read()

content = content.replace('</cloud-network>', '    <exclude domain="database" path="inbusiness.db" />\n        <exclude domain="database" path="inbusiness.db-wal" />\n        <exclude domain="database" path="inbusiness.db-shm" />\n    </cloud-network>')

with open('app/src/main/res/xml/data_extraction_rules.xml', 'w') as f:
    f.write(content)
