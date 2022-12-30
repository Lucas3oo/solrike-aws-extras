# solrike-aws-extras
Util classes for AWS. Depends on AWS SDK v2.

Java SQL Datasource factory which enables IAM authentication with AWS RDS like MySQL on any [Hikari datasource](https://github.com/brettwooldridge/HikariCP). That means password-less access from the application towards the DB.

In the src/test folder there are some samples on how to use the factory for
[Spring](./src/test/java/se/solrike/awsrdsiamdatasourcefactory/sample/RdsIamDatasourceFactoryForSpring.java) and [Micronaut](./src/test/java/se/solrike/awsrdsiamdatasourcefactory/sample/RdsIamDatasourceFactoryForMicronaut.java).

This library isn't dependent on Spring or Micronaut but the tests in it are. It only depends on Hikari and AWS RDS libs.

The reason for the actual implementation of datasource factory is left in test folder is that for Micronaut you need the annotation processor to cruch the code so it is better that the application that uses this lib implements this factory instead. But feel free to copy the code. It is basically only the wiring you need to do.

# Datasource factory for IAM authentication

## Setup step by step

1) Create AWS RDS. Make sure to enable IAM authentication.

2) Create the DB and the user that shall be used by the application

```sql
CREATE DATABASE database1;

CREATE USER myAppDbUser IDENTIFIED WITH AWSAuthenticationPlugin AS 'RDS';
GRANT CREATE, DELETE, INSERT, SELECT, UPDATE, SHOW VIEW ON database1.* TO myAppDbUser;
```

3) Create an IAM role. The role that the application that connects to the DB must have permission to connect to the DB with IAM
authentication.

Typical permission for the role:


```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "rds-db:connect",
            "Resource": "arn:aws:rds-db:eu-north-1:112233428224:dbuser:db-JF2MKOSKOSNFKOSIKQKOS7EXX4/myAppDbUser"
        }
    ]
}
```

In above sample the db-JF2MKOSKOSNFKOSIKQKOS7EXX4 is the resource ID of the DB.

4) Configure your Java app. Configure the database connection as usual but now you don't have to specify any password.
And the process must run with the role that has the correct permission. Either using access key/secret key or
using ["instance profiles"](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2_instance-profiles.html) (i.e. assign a role to the EC2 or ECS task).

5) Override the default datasource factory. The two samples are ready to go. Just copy any to your code base.
The GAV for the library is:

```gradle
implementation 'se.solrike.aws:solrike-aws-extras:0.1.0'
```

# Release Notes
## 0.1.0
Supports datasource factory that creates IAM authentication enabled Hikari datasources

