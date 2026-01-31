#!/bin/bash
set -e

# 1. Primary DB 대기 (호스트: mysql_primary)
echo "==> Waiting for Primary (mysql_primary) to be ready..."
until mysql -h mysql_primary -u root -p'rootpw123!' -e "SELECT 1;" > /dev/null 2>&1; do
  echo "Waiting for Primary..."
  sleep 3
done

# 2. Replica DB 대기 (호스트: mysql_replica)
# 주의: 사이드카에서 접속하므로 -h mysql_replica 필수!
echo "==> Waiting for Replica (mysql_replica) to be ready..."
until mysql -h mysql_replica -u root -p'rootpw123!' -e "SELECT 1;" > /dev/null 2>&1; do
  echo "Waiting for Replica..."
  sleep 3
done

# 2. Replica2 DB 대기 (호스트: mysql_replica)
# 주의: 사이드카에서 접속하므로 -h mysql_replica 필수!
echo "==> Waiting for Replica2 (mysql_replica) to be ready..."
until mysql -h mysql_replica2 -u root -p'rootpw123!' -e "SELECT 1;" > /dev/null 2>&1; do
  echo "Waiting for Replica..."
  sleep 3
done


# 3. 복제 연결 설정
echo "==> Configuring Replication(1) and Read-Only mode..."
# 접속 대상: -h mysql_replica
mysql -h mysql_replica -u root -p'rootpw123!' <<EOF
STOP REPLICA;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql_primary',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='replpw123!',
  SOURCE_AUTO_POSITION=1;
START REPLICA;

SET GLOBAL super_read_only=ON;
EOF

# 3. 복제 연결 설정
echo "==> Configuring Replication(2) and Read-Only mode..."
# 접속 대상: -h mysql_replica2
mysql -h mysql_replica2 -u root -p'rootpw123!' <<EOF
STOP REPLICA;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql_primary',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='replpw123!',
  SOURCE_AUTO_POSITION=1;
START REPLICA;

SET GLOBAL super_read_only=ON;
EOF


echo "==> Replication started and Read-Only mode enabled!"
