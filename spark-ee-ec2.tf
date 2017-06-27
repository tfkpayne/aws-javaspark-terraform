provider "aws" {
  region = "eu-central-1"
}

resource "aws_security_group" "spark" {
  name        = "spark_security_group"
  description = "Security Group for Spark instances "

  # SSH access from anywhere
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTP access from the LB
  ingress {
    from_port   = 4567
    to_port     = 4567
    protocol    = "tcp"
    security_groups = ["${aws_security_group.spark_lb.id}"]
  }

  # outbound internet access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_key_pair" "auth" {
  key_name   = "tom-payne-ee-2"
  public_key = "${file("/Users/tom/Downloads/tom-payne-ee.pub")}"
}


resource "aws_instance" "spark_rest" {
  ami           = "ami-8da700e2"
  instance_type = "t2.micro"

  count = 2

  vpc_security_group_ids = ["${aws_security_group.spark.id}"]

  key_name = "${aws_key_pair.auth.id}"

  connection {
    user = "ec2-user"
    private_key = "${file("/Users/tom/Downloads/tom-payne-ee.pem")}"
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir -p /home/ec2-user/.aws"
    ]
  }

  provisioner "file" {
    source = "/Users/tom/.aws/credentials"
    destination = "/home/ec2-user/.aws/credentials"
  }

  provisioner "file" {
    source = "target/spark-rest-1.0-SNAPSHOT-jar-with-dependencies.jar"
    destination = "/var/tmp/spark-rest.jar"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo yum -y update",
      "sudo yum -y install java-1.8.0",
      "sudo alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java",
      "echo Completed Java Installation",
      "nohup java -jar /var/tmp/spark-rest.jar &",
      "echo Completed Spark Startup",
      "sleep 1"
    ]
  }

}

# A security group for the ELB so it is accessible via the web
resource "aws_security_group" "spark_lb" {
  name        = "tom_spark_lb"

  # HTTP access from anywhere
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # outbound internet access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_elb" "spark_lb" {
  name = "tom-spark-lb"

  security_groups = ["${aws_security_group.spark_lb.id}"]
  instances       = ["${aws_instance.spark_rest.*.id}"]
  availability_zones = ["eu-central-1a", "eu-central-1b", "eu-central-1c"]

  listener {
    instance_port     = 4567
    instance_protocol = "http"
    lb_port           = 80
    lb_protocol       = "http"
  }

  provisioner "local-exec" {
    command = "echo ${aws_elb.spark_lb.dns_name}"
  }
}

resource "aws_s3_bucket" "spark_page_bucket" {
  provider = "aws"

  bucket = "spark-page-bucket"
  acl = "private"
}

resource "aws_s3_bucket_object" "pages" {
  provider = "aws"

  bucket = "${aws_s3_bucket.spark_page_bucket.id}"
  key    = "pages.json"
  source = "${path.module}/src/main/resources/pages.json"
}