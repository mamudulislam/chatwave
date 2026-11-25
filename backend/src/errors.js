'use strict';

class HttpError extends Error {
  constructor(status, code, message) {
    super(message || code);
    this.status = status;
    this.code = code;
  }
}

function badRequest(code = 'bad_request', message) {
  throw new HttpError(400, code, message);
}

function unauthorized(code = 'unauthorized', message) {
  throw new HttpError(401, code, message);
}

function forbidden(code = 'forbidden', message) {
  throw new HttpError(403, code, message);
}

function notFound(code = 'not_found', message) {
  throw new HttpError(404, code, message);
}

function conflict(code = 'conflict', message) {
  throw new HttpError(409, code, message);
}

function tooMany(code = 'rate_limited', message) {
  throw new HttpError(429, code, message);
}

module.exports = { HttpError, badRequest, unauthorized, forbidden, notFound, conflict, tooMany };
