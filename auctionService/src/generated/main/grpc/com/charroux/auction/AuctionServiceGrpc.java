package com.charroux.auction;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: auctionService.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AuctionServiceGrpc {

  private AuctionServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.charroux.AuctionService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.charroux.auction.Bidding,
      com.charroux.auction.BidResponse> getCarAuctionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CarAuction",
      requestType = com.charroux.auction.Bidding.class,
      responseType = com.charroux.auction.BidResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.charroux.auction.Bidding,
      com.charroux.auction.BidResponse> getCarAuctionMethod() {
    io.grpc.MethodDescriptor<com.charroux.auction.Bidding, com.charroux.auction.BidResponse> getCarAuctionMethod;
    if ((getCarAuctionMethod = AuctionServiceGrpc.getCarAuctionMethod) == null) {
      synchronized (AuctionServiceGrpc.class) {
        if ((getCarAuctionMethod = AuctionServiceGrpc.getCarAuctionMethod) == null) {
          AuctionServiceGrpc.getCarAuctionMethod = getCarAuctionMethod =
              io.grpc.MethodDescriptor.<com.charroux.auction.Bidding, com.charroux.auction.BidResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CarAuction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.Bidding.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.BidResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuctionServiceMethodDescriptorSupplier("CarAuction"))
              .build();
        }
      }
    }
    return getCarAuctionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.charroux.auction.CarModelsToBeRented> getCarModelsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CarModels",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.charroux.auction.CarModelsToBeRented.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.charroux.auction.CarModelsToBeRented> getCarModelsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.charroux.auction.CarModelsToBeRented> getCarModelsMethod;
    if ((getCarModelsMethod = AuctionServiceGrpc.getCarModelsMethod) == null) {
      synchronized (AuctionServiceGrpc.class) {
        if ((getCarModelsMethod = AuctionServiceGrpc.getCarModelsMethod) == null) {
          AuctionServiceGrpc.getCarModelsMethod = getCarModelsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.charroux.auction.CarModelsToBeRented>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CarModels"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.charroux.auction.CarModelsToBeRented.getDefaultInstance()))
              .setSchemaDescriptor(new AuctionServiceMethodDescriptorSupplier("CarModels"))
              .build();
        }
      }
    }
    return getCarModelsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuctionServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuctionServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuctionServiceStub>() {
        @java.lang.Override
        public AuctionServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuctionServiceStub(channel, callOptions);
        }
      };
    return AuctionServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuctionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuctionServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuctionServiceBlockingStub>() {
        @java.lang.Override
        public AuctionServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuctionServiceBlockingStub(channel, callOptions);
        }
      };
    return AuctionServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuctionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuctionServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuctionServiceFutureStub>() {
        @java.lang.Override
        public AuctionServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuctionServiceFutureStub(channel, callOptions);
        }
      };
    return AuctionServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default io.grpc.stub.StreamObserver<com.charroux.auction.Bidding> carAuction(
        io.grpc.stub.StreamObserver<com.charroux.auction.BidResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getCarAuctionMethod(), responseObserver);
    }

    /**
     */
    default void carModels(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.charroux.auction.CarModelsToBeRented> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCarModelsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AuctionService.
   */
  public static abstract class AuctionServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuctionServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AuctionService.
   */
  public static final class AuctionServiceStub
      extends io.grpc.stub.AbstractAsyncStub<AuctionServiceStub> {
    private AuctionServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuctionServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuctionServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.charroux.auction.Bidding> carAuction(
        io.grpc.stub.StreamObserver<com.charroux.auction.BidResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getCarAuctionMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void carModels(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.charroux.auction.CarModelsToBeRented> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCarModelsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AuctionService.
   */
  public static final class AuctionServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuctionServiceBlockingStub> {
    private AuctionServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuctionServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuctionServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.charroux.auction.CarModelsToBeRented carModels(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCarModelsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AuctionService.
   */
  public static final class AuctionServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuctionServiceFutureStub> {
    private AuctionServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuctionServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuctionServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.charroux.auction.CarModelsToBeRented> carModels(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCarModelsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CAR_MODELS = 0;
  private static final int METHODID_CAR_AUCTION = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CAR_MODELS:
          serviceImpl.carModels((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.charroux.auction.CarModelsToBeRented>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CAR_AUCTION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.carAuction(
              (io.grpc.stub.StreamObserver<com.charroux.auction.BidResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getCarAuctionMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.charroux.auction.Bidding,
              com.charroux.auction.BidResponse>(
                service, METHODID_CAR_AUCTION)))
        .addMethod(
          getCarModelsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.charroux.auction.CarModelsToBeRented>(
                service, METHODID_CAR_MODELS)))
        .build();
  }

  private static abstract class AuctionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuctionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.charroux.auction.AuctionServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AuctionService");
    }
  }

  private static final class AuctionServiceFileDescriptorSupplier
      extends AuctionServiceBaseDescriptorSupplier {
    AuctionServiceFileDescriptorSupplier() {}
  }

  private static final class AuctionServiceMethodDescriptorSupplier
      extends AuctionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuctionServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AuctionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuctionServiceFileDescriptorSupplier())
              .addMethod(getCarAuctionMethod())
              .addMethod(getCarModelsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
