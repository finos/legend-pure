/*
 * Copyright 2026 Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * pure_rust.h -- the C ABI of the Rust Pure evaluator (libpure_rust_ffi).
 *
 * This header is the contract between the evaluator and every host that
 * drives it. It is written by hand and reviewed as a specification; the
 * crate's exported symbols are tested against the declarations below, in
 * both directions. A client written from this header alone is complete.
 *
 * THE RULES
 *
 * 1. Only C data crosses. Integers, UTF-8 bytes with their length, opaque
 *    handles and the flat structs declared here. Never a Rust type, never
 *    a host type. A string is a pointer plus a byte count; it is never
 *    NUL-terminated and never read past its count. Inside a struct a
 *    string whose pointer is NULL is absent (the host's null); a non-NULL
 *    pointer with a count of zero is the empty string. The bytes must be
 *    valid UTF-8; an invalid sequence is a failure, not a conversion.
 *
 * 2. Each side owns what it allocates and frees it through its own
 *    release. The host owns the description structs, slots, property
 *    lists and strings it passes in; Rust reads them only while the call
 *    that received them is running and never keeps a pointer to them.
 *    Rust owns every pure_result it returns and the buffer pure_last_failure
 *    points into; the host releases a result with pure_result_free and
 *    never frees Rust memory any other way. The host never allocates on
 *    Rust's behalf.
 *
 * 3. Failures are a status plus a payload the host fetches. Every
 *    function that can fail returns a pure_status; when it is not
 *    PURE_STATUS_OK the host reads pure_last_failure() on the same thread,
 *    before its next call into the library. No unwind ever crosses the
 *    boundary in either direction: every exported function runs inside a
 *    panic guard; a panic inside one poisons its context (every later
 *    running call on that context fails with a message saying so, and
 *    the host should free it and create another) and is reported as a
 *    failure of kind PURE_FAILURE_PANIC. A host callback returns a
 *    status and never unwinds, longjmps or throws through Rust.
 *
 * 4. One context is entered by one thread at a time. The host serialises
 *    its calls on a context. A call made while another call on the same
 *    context is still running (for instance from inside a callback) is
 *    refused with a failure, never blocked. Different contexts are
 *    independent; a context may move between threads between calls.
 *
 * 5. A host node id is stable until the host is told to forget it. The
 *    host names its nodes by int64 ids of its choosing; Rust holds them
 *    for as long as something on its side refers to the node, and tells
 *    the host through the forget callback which ids it no longer holds.
 *    After that the host may reuse or drop them. Describing the same host
 *    object again later may use a fresh id.
 *
 * 6. Callbacks happen only from inside an entered call, on the thread
 *    that made it (forget also from inside pure_context_free), and never
 *    after pure_context_free returns. A callback must not call back into
 *    the context it was made from (rule 4); it may call the pure_reply_*
 *    functions on the reply it was handed, which is the only way to
 *    answer, and it may read pure_abi_version.
 *
 * 7. The ABI is versioned by one function. PURE_ABI_VERSION below is the
 *    version this header describes; pure_abi_version() is the version the
 *    loaded library implements. A client checks they are equal at load
 *    time, before any other call, and refuses to proceed otherwise.
 *    Version 1 is not stable until the library's first release: until
 *    then this header may change without its number moving. From the
 *    first release on, any change to it bumps PURE_ABI_VERSION.
 *
 * 8. Per-thread state. pure_last_failure and pure_last_timings describe
 *    the last call made on the calling thread. They are read on that
 *    thread, right after the call they describe.
 *
 * TEST SUPPORT
 *
 * The section of that name at the end of this header (pure_property, the
 * PURE_CROSSING_* constants, pure_test_object_new, pure_test_crossing) is
 * outside the contract: it serves the boundary tests of the library's own
 * clients, follows the rules above, and may change or go in any version.
 * A host never builds on it.
 *
 * LAYOUT
 *
 * Every struct is declared with its 8-byte fields first, then 4-byte
 * fields, so there is no interior padding; sizes on a 64-bit target are
 * asserted at the end of this header and mirrored in the Rust crate.
 * Every struct has 8-byte alignment, and a host allocates the ones it
 * passes by pointer (descriptions, slots, properties, the callback
 * table, the timings) with that alignment: a struct layout does, a plain
 * byte buffer may not, and Rust reads them in place. All enumerations
 * below are integer constants; the fields that carry them are typed
 * int32_t, never an enum type.
 */

#ifndef PURE_RUST_H
#define PURE_RUST_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* The ABI version this header describes. See rule 7. */
#define PURE_ABI_VERSION 1

/* ---------------------------------------------------------------------
 * Statuses and constants.
 * ------------------------------------------------------------------ */

/* Returned by every function that can fail, and by every host callback. */
enum {
    PURE_STATUS_OK = 0,
    /* The call failed; pure_last_failure() says how. From a callback:
     * the host could not answer and said why through pure_reply_fail. */
    PURE_STATUS_FAILED = 1,
    /* Callbacks only: the host has nothing for this question (no such
     * node or property; a node that is not a closure). */
    PURE_STATUS_ABSENT = 2
};

/* The kind of a pure_description (what the host hands in) and of a
 * pure_leaf (what Rust hands out). The numbers of the description kinds
 * are the bridge's own and are shared with every host. */
enum {
    PURE_KIND_BOOLEAN = 0,          /* payload "true" / "false"          */
    PURE_KIND_INTEGER = 1,          /* integer (payload: beyond 64 bits) */
    PURE_KIND_FLOAT = 2,            /* real                              */
    PURE_KIND_DECIMAL = 3,          /* payload: plain decimal text       */
    PURE_KIND_STRING = 4,           /* payload: the string               */
    PURE_KIND_DATE = 5,             /* year..second, precision, subsecond*/
    PURE_KIND_LATEST_DATE = 6,      /* %latest                           */
    PURE_KIND_STRICT_TIME = 7,      /* hour..second, precision, subsecond*/
    PURE_KIND_BYTE = 8,             /* not yet carried; a failure        */
    PURE_KIND_ENUM_VALUE = 10,      /* classifier = enumeration path,
                                       name = the value's name           */
    PURE_KIND_PACKAGEABLE_ELEMENT = 11, /* user_path set                 */
    PURE_KIND_NODE = 12,            /* any other host node, by id        */
    PURE_KIND_RUST_OBJECT = 13,     /* id = a handle Rust issued         */
    PURE_KIND_MAP = 14              /* leaves only: a map, as entries    */
};

/* Date and time precision, in descriptions and leaves. */
enum {
    PURE_PRECISION_YEAR = 0,
    PURE_PRECISION_MONTH = 1,
    PURE_PRECISION_DAY = 2,
    PURE_PRECISION_HOUR = 3,
    PURE_PRECISION_MINUTE = 4,
    PURE_PRECISION_SECOND = 5,
    PURE_PRECISION_SUBSECOND = 6
};

/* The kind of a failure (pure_failure.kind), and the kinds a host may
 * report through pure_reply_fail. */
enum {
    PURE_FAILURE_EXECUTION_ERROR = 0,
    PURE_FAILURE_ASSERTION_FAILED = 1,
    PURE_FAILURE_CONSTRAINT_VIOLATION = 2,
    PURE_FAILURE_FUNCTION_NOT_FOUND = 3,
    PURE_FAILURE_UNSUPPORTED = 4,
    PURE_FAILURE_NOT_IMPLEMENTED = 5,
    /* A panic inside the library; the context it happened on is
     * poisoned (rule 3). Never reported by a host. */
    PURE_FAILURE_PANIC = 6
};

/* ---------------------------------------------------------------------
 * Flat data.
 * ------------------------------------------------------------------ */

/* UTF-8 bytes with their count. See rule 1. */
typedef struct pure_string {
    const char *ptr;
    size_t len;
} pure_string;

/* One node of the host's graph, described by the host. Twenty-two fields.
 *
 * A zero-filled struct reads as a Boolean with definition 0: set `kind`
 * and `definition` on every description (definition is -1 when the node
 * stands for no lambda definition). Which fields matter depends on the
 * kind: INTEGER uses `integer`, or, for a value that does not fit 64
 * bits, `payload` holding its decimal digits (which Rust cannot hold yet
 * and reports unsupported); FLOAT uses `real`; the other primitives use
 * `payload` (dates and times their components instead); ENUM_VALUE uses
 * `classifier` and `name`; PACKAGEABLE_ELEMENT uses `user_path` (and
 * `classifier`, `name`); NODE uses `id`,
 * `classifier`, `name` and, for a lambda evaluated in a scope,
 * `definition`, the id of the lambda definition it is one evaluation of
 * (the closure cache is keyed by it); RUST_OBJECT uses `id` as the handle
 * Rust issued. The source fields anchor failures: `source_id` absent
 * means no anchor. */
typedef struct pure_description {
    int64_t id;
    int64_t definition;
    int64_t integer;         /* INTEGER                              */
    double real;             /* FLOAT                                */
    pure_string classifier;
    pure_string name;
    pure_string user_path;
    pure_string payload;
    pure_string subsecond;   /* digits, when precision is SUBSECOND  */
    pure_string source_id;
    int32_t kind;            /* PURE_KIND_*                          */
    int32_t year;
    int32_t month;           /* 0 when absent                        */
    int32_t day;
    int32_t hour;
    int32_t minute;
    int32_t second;
    int32_t precision;       /* PURE_PRECISION_*                     */
    int32_t line;
    int32_t column;
    int32_t end_line;
    int32_t end_column;
} pure_description;

/* One argument of a call, or the values of one property: a Pure value
 * of any multiplicity. `count` 0 is the empty value, 1 a single value,
 * more a collection. `items` may be NULL only when `count` is 0. */
typedef struct pure_slot {
    const pure_description *items;
    size_t count;
} pure_slot;

/* One value on its way out: a result flattens into leaves, one per
 * value, collections flattened and the empty value dropped.
 *
 * Which fields are set depends on `kind`:
 *   BOOLEAN       integer 0 or 1
 *   INTEGER       integer
 *   FLOAT         real (finite)
 *   DECIMAL       text: plain decimal text
 *   STRING        text
 *   LATEST_DATE   nothing
 *   DATE          year..second, precision, text: subsecond digits
 *                 (present and empty below SUBSECOND precision)
 *   STRICT_TIME   hour..second, precision, text: subsecond digits
 *   PACKAGEABLE_ELEMENT  text: the element's user path, which the host
 *                 resolves in its own graph
 *   ENUM_VALUE    text: the enumeration's user path; name: the value
 *   NODE          integer: the host's id of a node it handed in
 *   RUST_OBJECT   integer: a handle the host now holds (release it with
 *                 pure_object_release); text: the classifier's user path
 *   MAP           entries: 2 * entry_count leaves, each key followed by
 *                 its value; a key or value is itself one leaf of any
 *                 kind, MAP included
 * Every string of a leaf not listed for its kind is absent (NULL). */
typedef struct pure_leaf {
    int64_t integer;
    double real;
    pure_string text;
    pure_string name;
    const struct pure_leaf *entries;
    size_t entry_count;
    int32_t kind;            /* PURE_KIND_*                          */
    int32_t precision;       /* PURE_PRECISION_*                     */
    int32_t year;
    int32_t month;
    int32_t day;
    int32_t hour;
    int32_t minute;
    int32_t second;
} pure_leaf;

/* A result Rust owns: `count` leaves. Valid until pure_result_free. */
typedef struct pure_result {
    const pure_leaf *leaves;
    size_t count;
} pure_result;

/* A span of the host's sources: the source id and the start and end
 * positions, in the host's own numbering, of the description the span
 * came from (its line and column are the start). An absent `source`
 * (NULL) is no span; every position is then 0. */
typedef struct pure_span {
    pure_string source;
    int32_t start_line;
    int32_t start_column;
    int32_t end_line;
    int32_t end_column;
} pure_span;

/* One frame of a call stack: the called function's name and the span of
 * the call. */
typedef struct pure_frame {
    pure_string function;
    pure_span span;
} pure_frame;

/* A failure, as pure_last_failure describes it. `message` is the
 * human-readable line: the kind's name, ": ", then the text
 * ("FunctionNotFound: a::b", "AssertionFailed: ..."); `info` is the text
 * alone. `source` is where the failure happened, absent when it has no
 * span. `call_stack` holds `call_stack_len` frames, innermost first.
 * The three constraint fields are set for CONSTRAINT_VIOLATION only:
 * the constraint's id, its axis ("CLASS", "PRE", "POST") and the
 * `::`-joined path of the owning class or function. */
typedef struct pure_failure {
    pure_string message;
    pure_string info;
    pure_span source;
    pure_string constraint_id;
    pure_string constraint_kind;
    pure_string owner;
    const pure_frame *call_stack;
    size_t call_stack_len;
    int32_t kind;            /* PURE_FAILURE_*                       */
} pure_failure;

/* Where the last pure_start on this thread spent its time. */
typedef struct pure_timings {
    uint64_t lower_ns;         /* lowering the call (phases A and B)  */
    uint64_t run_ns;           /* running it                          */
    uint64_t run_host_reads;   /* host reads made while it ran        */
    uint64_t lower_host_reads; /* host reads made by the lowering     */
} pure_timings;

/* ---------------------------------------------------------------------
 * Opaque handles.
 * ------------------------------------------------------------------ */

/* One evaluator: its model, its registry, its state between calls and
 * the host it reads through. Created by pure_context_new, released by
 * pure_context_free. */
typedef struct pure_context pure_context;

/* The reply a callback fills. Valid only inside the callback it was
 * handed to; Rust owns it. */
typedef struct pure_reply pure_reply;

/* ---------------------------------------------------------------------
 * The host callback table.
 *
 * Rust asks the host about the nodes it handed in, by id. Every callback
 * receives the `host` pointer of the table it belongs to and a reply to
 * fill; a string argument is UTF-8 bytes plus a count. A callback answers
 * by pushing into the reply and returning PURE_STATUS_OK, by returning
 * PURE_STATUS_ABSENT, or by calling pure_reply_fail and returning
 * PURE_STATUS_FAILED. Rust copies everything pushed before the push
 * returns, so host memory handed to a pure_reply_* function need only
 * stay valid for that push.
 * ------------------------------------------------------------------ */

/* Every value of property `name` of node `id`, pushed as descriptions.
 * ABSENT: no such node or property. */
typedef int32_t (*pure_property_values_fn)(void *host, int64_t id,
                                           const char *name, size_t name_len,
                                           pure_reply *reply);

/* The values of the variable `name` that the lambda node `id` captured
 * from the scope it was evaluated in, pushed as descriptions. OK with no
 * push: it is such a closure but holds no variable of that name.
 * ABSENT: the node is not a closure evaluated in a scope. */
typedef int32_t (*pure_captured_values_fn)(void *host, int64_t id,
                                           const char *name, size_t name_len,
                                           pure_reply *reply);

/* The property keys of node `id`, pushed as names. ABSENT: no such node
 * (read as no keys). */
typedef int32_t (*pure_keys_fn)(void *host, int64_t id, pure_reply *reply);

/* `count` node ids nothing on the Rust side holds any more (rule 5). */
typedef void (*pure_forget_fn)(void *host, const int64_t *ids, size_t count);

typedef struct pure_host_table {
    void *host;                             /* handed back to every callback */
    pure_property_values_fn property_values; /* required                     */
    pure_captured_values_fn captured_values; /* required                     */
    pure_keys_fn keys;                       /* required                     */
    pure_forget_fn forget;                   /* may be NULL: never told      */
} pure_host_table;

/* ---------------------------------------------------------------------
 * Functions.
 *
 * `ctx` is a context from pure_context_new that pure_context_free has not
 * released; NULL is refused with a failure, never dereferenced. Out
 * pointers are written only on PURE_STATUS_OK. Running calls (pure_start,
 * the pure_object_* reads, and the two of the test support section) are
 * refused on a poisoned context; housekeeping calls
 * (pure_live_handle_count, pure_object_release, pure_context_free) still
 * work on one.
 * ------------------------------------------------------------------ */

/* The ABI version the loaded library implements (rule 7). */
uint32_t pure_abi_version(void);

/* A context over the bootstrap model and the native registry, bound to
 * the callbacks in `table`, which is copied: it need not outlive the
 * call. Fails when the table or a required callback is NULL. */
int32_t pure_context_new(const pure_host_table *table, pure_context **out);

/* Release a context: every Rust object the host still holds a handle to,
 * the model, the state. The host nodes that die with them are reported
 * through `forget` before it returns (rule 5), the last callback the host
 * sees. Fails (and releases nothing) while a call on the context is
 * running. After it returns OK no callback is made and `ctx` is never
 * passed again. NULL is accepted and does nothing. */
int32_t pure_context_free(pure_context *ctx);

/* Run `function` (a lambda, a native function or a function definition,
 * described by the host) over `argc` argument slots. The result is a
 * Rust-owned array of leaves the host materialises and then frees with
 * pure_result_free. */
int32_t pure_start(pure_context *ctx, const pure_description *function,
                   const pure_slot *args, size_t argc, pure_result **out);

/* Release a result returned by any function of this ABI. NULL is
 * accepted and does nothing. */
void pure_result_free(pure_result *result);

/* Every value of property `name` of the Rust object behind `handle`, as
 * leaves; an unset property is an empty result. Fails for a handle the
 * host no longer holds. */
int32_t pure_object_property_values(pure_context *ctx, int64_t handle,
                                    const char *name, size_t name_len,
                                    pure_result **out);

/* The property names of the Rust object behind `handle`, as STRING
 * leaves. */
int32_t pure_object_property_names(pure_context *ctx, int64_t handle,
                                   pure_result **out);

/* The classifier user path of the Rust object behind `handle`, as one
 * STRING leaf. */
int32_t pure_object_classifier(pure_context *ctx, int64_t handle,
                               pure_result **out);

/* Drop the reference the host held for `handle`. Idempotent; a handle
 * never issued is ignored. */
void pure_object_release(pure_context *ctx, int64_t handle);

/* Host nodes Rust still holds plus Rust objects the host still holds:
 * a host asserts this returns to zero. -1 while a call on the context is
 * running. */
int64_t pure_live_handle_count(pure_context *ctx);

/* Where the last pure_start on this thread spent its time. */
void pure_last_timings(pure_timings *out);

/* The failure of the last call on this thread, or NULL when it
 * succeeded. It is set by pure_context_new and by every function that
 * takes a context (cleared when they return OK, or succeed), and left
 * untouched by pure_result_free, pure_last_timings, pure_abi_version,
 * the pure_reply_* functions and this one. The struct and everything it
 * points to are Rust-owned and valid until the next call that sets it
 * on this thread. */
const pure_failure *pure_last_failure(void);

/* Push `count` descriptions into a reply (property_values and
 * captured_values callbacks). Pushes accumulate; copied before return. */
int32_t pure_reply_descriptions(pure_reply *reply, const pure_description *items,
                                size_t count);

/* Push `count` names into a reply (keys callback). Pushes accumulate;
 * copied before return. */
int32_t pure_reply_names(pure_reply *reply, const pure_string *items,
                         size_t count);

/* Record why the callback failed: a PURE_FAILURE_* kind (FUNCTION_NOT_FOUND,
 * UNSUPPORTED and NOT_IMPLEMENTED keep their kind on the Rust side; any
 * other kind becomes an execution error carrying `message`) and the text.
 * A recorded failure wins over the status the callback then returns. */
int32_t pure_reply_fail(pure_reply *reply, int32_t kind, const char *message,
                        size_t message_len);

/* ---------------------------------------------------------------------
 * Test support: outside the contract (see TEST SUPPORT above).
 * ------------------------------------------------------------------ */

/* One property of an object pure_test_object_new allocates. */
typedef struct pure_property {
    pure_string name;
    pure_slot values;
} pure_property;

/* What a described node became on the Rust side (pure_test_crossing). */
enum {
    PURE_CROSSING_ELEMENT = 0,     /* unified with a model element      */
    PURE_CROSSING_ENUM_VALUE = 1,  /* a value of a known enumeration    */
    PURE_CROSSING_HOST_NODE = 2,   /* a host node read on demand        */
    PURE_CROSSING_RUST_OBJECT = 3, /* a Rust object the host holds      */
    PURE_CROSSING_PRIMITIVE = 4    /* decoded to a primitive            */
};

/* Allocate a Rust object of the classifier named by `classifier`, with
 * `count` properties, and hand the host its handle; the host releases it
 * with pure_object_release. This is not Pure's `new`: the classifier is
 * not checked against the model, and no default is applied and no
 * constraint run. */
int32_t pure_test_object_new(pure_context *ctx, const char *classifier,
                             size_t classifier_len,
                             const pure_property *properties, size_t count,
                             int64_t *out_handle);

/* Which Rust shape `node` becomes once described (PURE_CROSSING_*): the
 * node is crossed in and discarded. */
int32_t pure_test_crossing(pure_context *ctx, const pure_description *node,
                           int32_t *out);

/* ---------------------------------------------------------------------
 * Layout checks (64-bit targets). The Rust crate asserts the same sizes.
 * ------------------------------------------------------------------ */

#if defined(UINTPTR_MAX) && (UINTPTR_MAX == 0xFFFFFFFFFFFFFFFFu) && \
    defined(__STDC_VERSION__) && (__STDC_VERSION__ >= 201112L)
_Static_assert(sizeof(pure_string) == 16, "pure_string layout");
_Static_assert(sizeof(pure_description) == 176, "pure_description layout");
_Static_assert(sizeof(pure_slot) == 16, "pure_slot layout");
_Static_assert(sizeof(pure_property) == 32, "pure_property layout");
_Static_assert(sizeof(pure_leaf) == 96, "pure_leaf layout");
_Static_assert(sizeof(pure_result) == 16, "pure_result layout");
_Static_assert(sizeof(pure_span) == 32, "pure_span layout");
_Static_assert(sizeof(pure_frame) == 48, "pure_frame layout");
_Static_assert(sizeof(pure_failure) == 136, "pure_failure layout");
_Static_assert(sizeof(pure_timings) == 32, "pure_timings layout");
_Static_assert(sizeof(pure_host_table) == 40, "pure_host_table layout");
#endif

#ifdef __cplusplus
}
#endif

#endif /* PURE_RUST_H */
