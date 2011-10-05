from distutils.core import setup

setup(
    name='glassfish-rest-client',
    version='4.0',
    description='Python client for the RESTful GlassFish Administration interface',
    author='Oracle',
    author_email='dev@glassfish.java.net',
    url='http://glassfish.org',
    long_description="Python client for the RESTful GlassFish Administration interface",
    license='CDDL+GPLv2 w/CPE',
    packages=['glassfish', 'glassfish.rest'],
)